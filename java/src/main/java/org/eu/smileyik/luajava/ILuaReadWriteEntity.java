package org.eu.smileyik.luajava;

import java.io.*;
import java.nio.ByteBuffer;

public interface ILuaReadWriteEntity {

    /**
     * control C side alloc how many memory
     */
    public int bufferSize();

    /**
     * C side set latest allocated memory ptr
     */
    public void setDataPtr(long ptr);
    public long getDataPtr();

    /**
     * dump from lua
     * @param facade lua state
     * @param in c allocate bytebuffer, read bytes from here.
     */
    public void luaWrite(LuaStateFacade facade, ByteBuffer in) throws IOException;

    /**
     * load to lua
     * @param facade lua state
     * @param out c allocate bytebuffer, write bytes to here.
     * @return written bytes
     */
    public int luaRead(LuaStateFacade facade, ByteBuffer out) throws IOException;

    public static final class SimpleWrite implements ILuaReadWriteEntity, Closeable {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public int bufferSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDataPtr(long ptr) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDataPtr() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void luaWrite(LuaStateFacade facade, ByteBuffer in) throws IOException {
            int capacity = in.capacity();
            byte[] data = new byte[capacity];
            in.get(data);
            out.write(data, 0, capacity);
            out.flush();
        }

        @Override
        public int luaRead(LuaStateFacade facade, ByteBuffer out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        public OutputStream getOutputStream() {
            return out;
        }
    }

    public static final class SimpleRead implements ILuaReadWriteEntity, Closeable {
        private final InputStream in;
        private final int bufferSize;
        private long dataPtr = 0;

        public SimpleRead(InputStream in, int bufferSize) {
            this.in = in;
            this.bufferSize = bufferSize;
        }

        @Override
        public int bufferSize() {
            return bufferSize;
        }

        @Override
        public void setDataPtr(long dataPtr) {
            this.dataPtr = dataPtr;
        }

        @Override
        public long getDataPtr() {
            return dataPtr;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public void luaWrite(LuaStateFacade facade, ByteBuffer in) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int luaRead(LuaStateFacade facade, ByteBuffer out) throws IOException {
            int capacity = out.capacity();
            byte[] bytes = new byte[capacity];
            int len = in.read(bytes);
            out.put(bytes);
            return len;
        }
    }
}
