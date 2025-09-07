
COMMAND= ./gradlew

all: jar luajava4Luajit luajava4Lua52 luajava4Lua53 luajava4Lua54

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
