# luajava

This is a clone of the luajava repository.  
⭐ If you like this please let me know.⭐

## Changes to the original

* 🐭 Add some subclasses for LuaObject, like LuaTable, LuaArray.
* 🐸 Rewrite some LuaJavaAPI to support automatic boxing and unboxing, 
method overloading, number conversion priority and allow array style lua table 
covert to java array. cached reflect result.
* 🦝 Implementing finalize method by PhantomReference
* 🕊️ Remove the synchronized keyword and use ReentrantLock for synchronization
* 🐹 Enhanced LuaJavaAPI: You can use "#objectName" to get java array length or
size of Collection/Map instance; you also can use `..` to concat string with
java object. details example can be find in LuaJavaApiTest class.
* 🐼 Will print lua stack when lua error
* Lua 5.2, Lua 5.3, Lua 5.4 supported! then you can use **uservalue** not **metatable** to handle
java object function things. You can also use same metatable for all java
object instances.

 
## How to build it

I still try to write script to let build it be easier. 
At first, ensure your system installed **JDK 11+?**, **GNU C** OR **Clang** OR **Visual Studio**.
If you are using Linux, please install **make**.

**Note**: Gradle script using **javac** command to generate C header(luajava.h). If you are using **JDK1.8** you need change to **javap**, 

if you are Linux or Windows, just run command at terminal
```
# build native shared library
./gradlew :c:build 
# build luajava jar
./gradlew :java:build
```

if build succeed, all staff will be in `luajava/build/outputs` directory. 

## How to use it

Sadly, I was deleted the code of load shared native library. But I add it for tests.
If you just want to run some example, you can just build it, and gradle will start tests.

And you can view some examples at `luajava/java/src/test`.

# Original README

luajava
=======

LuaJava is a scripting tool for Java. The goal of this tool is to allow scripts written in Lua to manipulate components developed in Java. 

It allows Java components to be accessed from Lua using the same syntax that is used for accessing Lua`s native objects, without any need 
for declarations or any kind of preprocessing.  LuaJava also allows Java to implement an interface using Lua. This way any interface can be
implemented in Lua and passed as parameter to any method, and when called, the equivalent function will be called in Lua, and it's result 
passed back to Java.
