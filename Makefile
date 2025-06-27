
COMMAND= ./gradlew

all: luajava4Luajit luajava4Lua52 luajava4Lua53 luajava4Lua54

luajava4Luajit:
	$(COMMAND) :c:luajit:model
	$(COMMAND) :c:luajit:build

luajava4Lua52:
	$(COMMAND) :c:lua52:model
	$(COMMAND) :c:lua52:downloadlua-5.2.4
	$(COMMAND) :c:lua52:decompresslua-5.2.4Source
	$(COMMAND) :c:lua52:buildLua
	$(COMMAND) :c:lua52:build

luajava4Lua53:
	$(COMMAND) :c:lua53:model
	$(COMMAND) :c:lua53:downloadlua-5.3.6
	$(COMMAND) :c:lua53:decompresslua-5.3.6Source
	$(COMMAND) :c:lua53:buildLua
	$(COMMAND) :c:lua53:build

luajava4Lua54:
	$(COMMAND) :c:lua54:model
	$(COMMAND) :c:lua54:downloadlua-5.4.8
	$(COMMAND) :c:lua54:decompresslua-5.4.8Source
	$(COMMAND) :c:lua54:buildLua
	$(COMMAND) :c:lua54:build

clean:
	$(COMMAND) clean