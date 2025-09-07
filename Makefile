
COMMAND= ./gradlew

all: jar luajava4Luajit luajava4Lua52 luajava4Lua53 luajava4Lua54
testAll: testLuajit testLua52 testLua53 testLua54
testLuajit: clean luajava4Luajit test
testLua52: clean luajava4Lua52 test
testLua53: clean luajava4Lua53 test
testLua54: clean luajava4Lua54 test

luajava4Luajit:
	$(COMMAND) :c:luajit:model
	$(COMMAND) :c:luajit:build

luajava4Lua52:
	$(COMMAND) :c:lua52:build

luajava4Lua53:
	$(COMMAND) :c:lua53:build

luajava4Lua54:
	$(COMMAND) :c:lua54:build

jar:
	$(COMMAND) :java:build -x test

test:
	$(COMMAND) :java:test --tests "org.eu.smileyik.luajava.test2.**.*"

clean:
	$(COMMAND) clean
