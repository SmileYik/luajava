package org.eu.smileyik.luajava.debug.rsp;

import org.eu.smileyik.luajava.LuaState;
import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.breakPoint.BreakPoint;
import org.eu.smileyik.luajava.debug.rsp.command.hook.Command;
import org.eu.smileyik.luajava.debug.rsp.command.hook.ContinueCommand;
import org.eu.smileyik.luajava.debug.rsp.command.rsp.RspCommand;
import org.eu.smileyik.luajava.debug.util.AnsiMessageBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SimpleRspServer implements BiConsumer<LuaStateFacade, LuaDebug>, RspDebugServer {
    private static final Logger LOG = Logger.getLogger(SimpleRspServer.class.getName());

    private final int port;
    private boolean debugFlag = false;
    private final LuaStateFacade luaStateFacade;
    private final Set<BreakPoint> breakPoints = Collections.synchronizedSet(new HashSet<>());
    private final Lock commandLock = new ReentrantLock();
    private final LinkedList<Command> commands = new LinkedList<>();
    private final Condition commandReceive = commandLock.newCondition();
    private final Lock messageLock = new ReentrantLock();
    private final LinkedList<String> messageQueue = new LinkedList<>();
    private final Condition messageCondition = messageLock.newCondition();
    private final ExecutorService serverThread = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private OutputStream out;
    private boolean step = true;
    private Command continueType = ContinueCommand.INSTANCE;
    private LuaDebug currentDebugInfo;

    private SimpleRspServer(int port, LuaStateFacade luaStateFacade) {
        this.port = port;
        this.luaStateFacade = luaStateFacade;
        this.luaStateFacade.setDebugHook(this);
        this.luaStateFacade.getLuaState().setHook(LuaState.LUA_MASKLINE, 0);
    }

    public static DebugServer start(int port, LuaStateFacade luaStateFacade) throws ExecutionException, InterruptedException {
        CompletableFuture<DebugServer> future = new CompletableFuture<>();
        new Thread(()->{
            SimpleRspServer server = new SimpleRspServer(port, luaStateFacade);
            future.complete(server);
            server.start();
        }).start();
        return future.get();
    }

    public RspDebugServer debug() {
        this.debugFlag = true;
        return this;
    }

    private void start() {
        serverThread.execute(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                while (!Thread.currentThread().isInterrupted()) {
                    LOG.info("RSP server listening on port " + port);
                    try (Socket socket = server.accept()) {
                        LOG.info("Accepted connection from " + socket.getInetAddress());
                        connected(socket);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                LOG.info("RSP server closed");
            }
        });

    }

    private void connected(Socket socket) throws IOException {
        try (InputStream in = (socket.getInputStream());
             OutputStream out = (socket.getOutputStream())) {
            this.out = out;

            int readByte;
            while ((readByte = in.read()) != -1) {
                if (readByte == '+') {
                    continue;
                }
                if (readByte == '$') {
                    StringBuilder commandBuilder = new StringBuilder();
                    while ((readByte = in.read()) != '#') {
                        commandBuilder.append((char) readByte);
                    }
                    String command = commandBuilder.toString();
                    debug("Debug Command: " + command);
                    int checksumHigh = in.read();
                    int checksumLow = in.read();
                    out.write('+');
                    dispatchCommand(out, command);
                }
            }
        }
    }

    private void sendResponseAsync(String response) {
        executor.execute(() -> {
            try {
                sendResponse(out, response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private synchronized void sendResponse(OutputStream out, String response) throws IOException {
        // $<command>#<checksum>
        StringBuilder fullResponse = new StringBuilder("$");
        fullResponse.append(response);
        fullResponse.append("#");

        byte checksum = 0;
        for (int i = 1; i < fullResponse.length() - 1; i++) {
            checksum += (byte) fullResponse.charAt(i);
        }
        fullResponse.append(String.format("%02x", checksum & 0xff));
        debug("Debug Response: " + fullResponse);
        out.write(fullResponse.toString().getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private void dispatchCommand(OutputStream out, String command) throws IOException {
        String response = RspCommand.SIM_COMMAND_RESP.getOrDefault(command, "");
        if (!response.isEmpty()) {
            sendResponse(out, response);
            return;
        }

        try {
            String innerCommand = command;
            String[] innerParams = null;
            if (command.startsWith("qRcmd")) {
                // $qRcmd,my_custom_command my_text_data#...
                String[] args = decodeFromHex(command.substring("qRcmd,".length())).split(" ");
                String[] innerArgs = args.length > 1 ? args[1].split(",") : new  String[0];
                if (innerArgs.length > 0) {
                    String path = args[0] + "." + innerArgs[0];
                    innerParams = innerArgs;
                    if (RspCommand.COMMAND_MAP.containsKey(path)) {
                        innerCommand = path;
                    } else {
                        innerCommand = args[0];
                    }
                }
            } else if (command.startsWith("p")) {
                innerCommand = "p";
            }

            if (RspCommand.COMMAND_MAP.containsKey(innerCommand)) {
                response = RspCommand.COMMAND_MAP.get(innerCommand).handle(
                        this, innerCommand, innerParams
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage("ERROR: " + e.getMessage() + "\n");
            response = "";
        }

        sendResponse(out, response);
    }

    private String message(String message) {
        if (!message.replace(AnsiMessageBuilder.ANSI_RESET, "").endsWith("\n")) {
            message = message + "\n";
        }
        if (!message.trim().startsWith(AnsiMessageBuilder.ANSI_START)) {
            message = "\u001b[34m" + message + "\u001b[0m";
        }

        StringBuilder messageBuilder = new StringBuilder("O");
        message.chars().forEach(c -> messageBuilder.append(String.format("%02x", c)));
        return messageBuilder.toString();
    }

    private String decodeFromHex(String hex) {
        StringBuilder sb = new StringBuilder();
        int len = hex.length();
        for (int i = 0; i < len; i += 2) {
            sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        return sb.toString();
    }

    @Override
    public void accept(LuaStateFacade luaStateFacade, LuaDebug luaDebug) {
        boolean flag = step;
        if (!flag) {
            for (BreakPoint breakPoint : new HashSet<>(breakPoints)) {
                if (breakPoint.enable() && breakPoint.isInBreakPoint(luaStateFacade, luaDebug)) {
                    flag = true;
                    if (breakPoint.countDownRepeatTimes()) {
                        breakPoints.remove(breakPoint);
                    }
                    sendResponseAsync(message("BreakPoint: " + breakPoint));
                    break;
                }
            }
        }

        debug("----" + Objects.toString(luaDebug).replace("\n", "\\n"));
        if (flag) {
            synchronized (this) {
                this.currentDebugInfo = luaDebug;
                sendMessageAsync(getSourceLine(luaDebug));
            }
            sendResponseAsync("T05");
        }
        while (flag) {
            Command command = null;
            commandLock.lock();
            try {
                if (commands.isEmpty()) {
                    commandReceive.await();
                }
                command = commands.removeFirst();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                commandLock.unlock();
            }
            flag = command == null || !command.handle(this, luaStateFacade, luaDebug);
        }

    }

    private void debug(String message) {
        if (debugFlag) {
            LOG.info(message);
        }
    }

    private String getSourceLine(LuaDebug ar) {
        if (ar == null || ar.getSource() == null) {
            return AnsiMessageBuilder.builder()
                    .red("No debug info or no source info.")
                    .toMessage();
        }
        String message = AnsiMessageBuilder.builder()
                .green("line ")
                .bold().append(ar.getCurrentLine()).resetColor()
                .green(": ")
                .red("Could not find target source line.")
                .newLine()
                .toMessage();
        String source = ar.getSource();
        int idx = ar.getCurrentLine() - 1;
        if (idx < 0) {
            return message;
        }

        boolean flag = false;
        if (source.startsWith("@")) {
            try (Stream<String> lines = Files.lines(Paths.get(source.substring(1)))) {
                source = lines
                        .skip(idx)
                        .findFirst()
                        .orElse(message);
                flag = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String[] split = source.split("\n");
            if (split.length > idx) {
                source = split[idx];
                flag = true;
            }
        }

        if (flag) {
            message = AnsiMessageBuilder.builder()
                    .green("line ")
                    .bold().append(ar.getCurrentLine()).resetColor()
                    .green(": ")
                    .bold().append(source)
                    .resetColor()
                    .newLine()
                    .toMessage();
        }
        return message;
    }

    // ************** RSP Debug Server **************

    @Override
    public LuaDebug getCurrentDebugInfo() {
        return currentDebugInfo;
    }

    @Override
    public void addCommand(Command command) {
        commandLock.lock();
        try {
            commands.addLast(command);
            commandReceive.signalAll();
        } finally {
            commandLock.unlock();
        }
    }

    @Override
    public void sendMessage(String message) throws IOException {
        sendResponse(message(message));
    }

    @Override
    public void sendMessageAsync(String message) {
        sendResponseAsync(message(message));
    }

    @Override
    public void sendResponse(String payload) throws IOException {
        sendResponse(out, payload);
    }

    @Override
    public void setContinueType(Command continueType) {
        this.continueType = continueType;
    }

    @Override
    public Command getContinueType() {
        return continueType;
    }

    @Override
    public void waitFillMessage() throws InterruptedException, IOException {
        messageLock.lock();
        try {
            messageCondition.await();
            while (!messageQueue.isEmpty()) {
                sendMessage(messageQueue.removeFirst());
            }
        } finally {
            messageLock.unlock();
        }
    }

    @Override
    public void finishedFillMessage() {
        messageLock.lock();
        try {
            messageCondition.signalAll();
        } finally {
            messageLock.unlock();
        }
    }

    @Override
    public void fillMessageQueue(String message) {
        this.messageQueue.addLast(message);
    }

    // ************** Debug Server **************

    @Override
    public void close() {
        serverThread.shutdown();
    }

    @Override
    public DebugServer waitConnection() throws InterruptedException {
        commandLock.lock();
        try {
            commandReceive.await();
        } finally {
            commandLock.unlock();
        }
        return this;
    }

    @Override
    public boolean step() {
        return step;
    }

    @Override
    public void step(boolean flag) {
        this.step = flag;
    }

    @Override
    public void addBreakPoint(BreakPoint breakPoint) {
        this.breakPoints.add(breakPoint);
    }

    @Override
    public void removeBreakPoint(BreakPoint breakPoint) {
        this.breakPoints.remove(breakPoint);
    }

    @Override
    public LuaStateFacade getLuaStateFacade() {
        return luaStateFacade;
    }
}
