#ifndef LUAJAVA_LUAJAVA_API_H
#define LUAJAVA_LUAJAVA_API_H

#include <jni.h>
#include <lua.h>

/* Constant that is used to index the JNI Environment */
#define LUAJAVAJNIENVTAG          "__JNIEnv"
/* Defines whether the metatable is of a java Object */
#define LUAJAVAOBJECTIND          "__IsJavaObject"
/* Defines the lua State Index Property Name */
#define LUAJAVASTATEINDEX         "LuaJavaStateIndex"
/* Defines the java class object metatable Name */
#define LUAJAVA_METATABLE_CLASS   "LuaJavaMetatableClass"
/* Defines the java object metatable name */
#define LUAJAVA_METATABLE_OBJECT  "LuaJavaMetatableObject"
/* Defines the java array object metatable name */
#define LUAJAVA_METATABLE_ARRAY   "LuaJavaMetatableArray"
/* Index metamethod name */
#define LUAINDEXMETAMETHODTAG     "__index"
/* New index metamethod name */
#define LUANEWINDEXMETAMETHODTAG  "__newindex"
/* Garbage collector metamethod name */
#define LUAGCMETAMETHODTAG        "__gc"
/* __len */
#define LUA_LEN_METAMETHOD_TAG    "__len"
/* __eq */
#define LUA_EQ_METAMETHOD_TAG     "__eq"
/* __concat */
#define LUA_CONCAT_METAMETHOD_TAG "__concat"
/* Call metamethod name */
#define LUACALLMETAMETHODTAG      "__call"
/* Constant that defines where in the metatable should I place the function name
 */
#define LUAJAVAOBJFUNCCALLED      "__FunctionCalled"

/* if not define get meta table method, then use lua_getmetatable */
#ifndef LUAJAVA_GET_METATABLE
#undef LUAJAVA_FORCE_SAME_METATABLE_CLASS
#undef LUAJAVA_FORCE_SAME_METATABLE_OBJECT
#undef LUAJAVA_FORCE_SAME_METATABLE_ARRAY
#define LUAJAVA_GET_METATABLE( L , OBJ_IDX ) lua_getmetatable(L, OBJ_IDX)
#endif
/* if not define set meta table method, then use lua_setmetatable */
#ifndef LUAJAVA_SET_METATABLE
#undef LUAJAVA_FORCE_SAME_METATABLE_CLASS
#undef LUAJAVA_FORCE_SAME_METATABLE_OBJECT
#undef LUAJAVA_FORCE_SAME_METATABLE_ARRAY
#define LUAJAVA_SET_METATABLE( L , OBJ_IDX ) lua_setmetatable(L, OBJ_IDX)
#endif

/***************************************************************************
 *
 * $FC Function setupLuaJavaApi
 *
 * $ED Description
 *    initilization lua java api
 *
 * $EP Function Parameters
 *    None
 *
 * $FV Returned Value
 *    unsigned char as result
 *
 *$. **********************************************************************/

void setupLuaJavaApi(JNIEnv *env);

/***************************************************************************
 *
 * $FC Function newCPtr
 *
 * $ED Description
 *    new CPtr instance.
 *
 * $EP Function Parameters
 *    $P env     - JNIEnv
 *    $P peer    - pointer
 *
 * $FV Returned Value
 *    jobject.
 *
 *$. **********************************************************************/

jobject newCPtr(JNIEnv *env, jlong peer);

/***************************************************************************
 *
 * $FC Function getLuaStateIndex
 *
 * $ED Description
 *    Gets the luaState index
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    luaState index
 *
 *$. **********************************************************************/

lua_Number getLuaStateIndex(lua_State *L);

/***************************************************************************
 *
 * $FC Function objectIndex
 *
 * $ED Description
 *    Function to be called by the metamethod __index of the java object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int objectIndex(lua_State *L);

/***************************************************************************
 *
 * $FC Function objectIndexReturn
 *
 * $ED Description
 *    Function returned by the metamethod __index of a java Object. It is
 *    the actual function that is going to call the java method.
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int objectIndexReturn(lua_State *L);

/***************************************************************************
 *
 * $FC Function objectNewIndex
 *
 * $ED Description
 *    Function to be called by the metamethod __newindex of the java object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int objectNewIndex(lua_State *L);

/***************************************************************************
 *
 * $FC Function classIndex
 *
 * $ED Description
 *    Function to be called by the metamethod __index of the java class
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int classIndex(lua_State *L);

/***************************************************************************
 *
 * $FC Function arrayIndex
 *
 * $ED Description
 *    Function to be called by the metamethod __index of a java array
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int arrayIndex(lua_State *L);

/***************************************************************************
 *
 * $FC Function arrayNewIndex
 *
 * $ED Description
 *    Function to be called by the metamethod __newindex of a java array
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int arrayNewIndex(lua_State *L);

/***************************************************************************
 *
 * $FC Function arrayLength
 *
 * $ED Description
 *    Function to be called by the metamethod __len of a java array
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int arrayLength(lua_State *L);

/***************************************************************************
 *
 * $FC Function objectContainerSize
 *
 * $ED Description
 *    Function to be called by the metamethod __len of a java collection or map.
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int objectContainerSize(lua_State *L);

/***************************************************************************
 *
 * $FC Function javaObjectEquals
 *
 * $ED Description
 *    Function to be called by the metamethod __eq of java object.
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int javaObjectEquals(lua_State *L);

/***************************************************************************
 *
 * $FC Function javaStringConcat
 *
 * $ED Description
 *    Concat String with other object.
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int javaStringConcat(lua_State *L);

/***************************************************************************
 *
 * $FC Function GC
 *
 * $ED Description
 *    Function to be called by the metamethod __gc of the java object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int gc(lua_State *L);

/***************************************************************************
 *
 * $FC Function javaBindClass
 *
 * $ED Description
 *    Implementation of lua function luajava.BindClass
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int javaBindClass(lua_State *L);

/***************************************************************************
 *
 * $FC Function createProxy
 *
 * $ED Description
 *    Implementation of lua function luajava.createProxy.
 *    Transform a lua table into a java class that implements a list
 *  of interfaces
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int createProxy(lua_State *L);

/***************************************************************************
 *
 * $FC Function javaNew
 *
 * $ED Description
 *    Implementation of lua function luajava.new
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int javaNew(lua_State *L);

/***************************************************************************
 *
 * $FC Function javaNewInstance
 *
 * $ED Description
 *    Implementation of lua function luajava.newInstance
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int javaNewInstance(lua_State *L);

/***************************************************************************
 *
 * $FC Function javaLoadLib
 *
 * $ED Description
 *    Implementation of lua function luajava.loadLib
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int javaLoadLib(lua_State *L);

/***************************************************************************
 *
 * $FC pushJavaObject
 *
 * $ED Description
 *    Function to create a lua proxy to a java object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P javaObject - Java Object to be pushed on the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int pushJavaObject(lua_State *L, jobject javaObject);

/***************************************************************************
 *
 * $FC pushJavaArray
 *
 * $ED Description
 *    Function to create a lua proxy to a java array
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P javaObject - Java array to be pushed on the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int pushJavaArray(lua_State *L, jobject javaObject);

/***************************************************************************
 *
 * $FC pushJavaClass
 *
 * $ED Description
 *    Function to create a lua proxy to a java class
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P javaObject - Java Class to be pushed on the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function
 *
 *$. **********************************************************************/

int pushJavaClass(lua_State *L, jobject javaObject);

/***************************************************************************
 *
 * $FC isJavaObject
 *
 * $ED Description
 *    Returns 1 is given index represents a java object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P idx - index on the stack
 *
 * $FV Returned Value
 *    int - Boolean.
 *
 *$. **********************************************************************/

int isJavaObject(lua_State *L, int idx);

/***************************************************************************
 *
 * $FC isJavaObject
 *
 * $ED Description
 *    Returns 1 is given index represents a java object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P idx - index on the stack
 *
 * $FV Returned Value
 *    int - Boolean.
 *
 *$. **********************************************************************/

jboolean isJavaFunctionInstance(JNIEnv *env, jobject *obj);

/***************************************************************************
 *
 * $FC getStateFromCPtr
 *
 * $ED Description
 *    Returns the lua_State from the CPtr Java Object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P cptr - CPtr object
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function.
 *
 *$. **********************************************************************/

lua_State *getStateFromCPtr(JNIEnv *env, jobject cptr);

/***************************************************************************
 *
 * $FC resetCPtr
 *
 * $ED Description
 *    Returns the lua_State from the CPtr Java Object
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P cptr - CPtr object
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function.
 *
 *$. **********************************************************************/
void resetCPtr(JNIEnv *env, jobject cptr);

/***************************************************************************
 *
 * $FC luaJavaFunctionCall
 *
 * $ED Description
 *    function called by metamethod __call of instances of JavaFunctionWrapper
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P Stack - Parameters will be received by the stack
 *
 * $FV Returned Value
 *    int - Number of values to be returned by the function.
 *
 *$. **********************************************************************/

int luaJavaFunctionCall(lua_State *L);

/***************************************************************************
 *
 * $FC pushJNIEnv
 *
 * $ED Description
 *    function that pushes the jni environment into the lua state
 *
 * $EP Function Parameters
 *    $P env - java environment
 *    $P L - lua State
 *
 * $FV Returned Value
 *    void
 *
 *$. **********************************************************************/

void pushJNIEnv(JNIEnv *env, lua_State *L);

/***************************************************************************
 *
 * $FC getEnvFromState
 *
 * $ED Description
 *    auxiliary function to get the JNIEnv from the lua state
 *
 * $EP Function Parameters
 *    $P L - lua State
 *
 * $FV Returned Value
 *    JNIEnv * - JNI environment
 *
 *$. **********************************************************************/

JNIEnv *getEnvFromState(lua_State *L);

/***************************************************************************
 *
 * $FC generateLuaStateStack
 *
 * $ED Description
 *    generate current lua stack
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P stack_str - stack message
 *
 * $FV Returned Value
 *    void
 *
 *$. **********************************************************************/

void generateLuaStateStack(lua_State *L, char *stack_str);

/***************************************************************************
 *
 * $FC luajavaSetObjectFunctionCalled
 *
 * $ED Description
 *    set java method name to userdata object (java object).
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P objIdx - target object index in stack
 *    $P methodName - java method name.
 *
 * $FV Returned Value
 *    void
 *
 *$. **********************************************************************/

inline void luajavaSetObjectFunctionCalled(lua_State *L, int objIdx, const char *methodName);

/***************************************************************************
 *
 * $FC luajavaGetObjectFunctionCalled
 *
 * $ED Description
 *    set java method name to userdata object (java object).
 *
 * $EP Function Parameters
 *    $P L - lua State
 *    $P objIdx - target object index in stack
 *
 * $FV Returned Value
 *    void
 *
 *$. **********************************************************************/

inline const char* luajavaGetObjectFunctionCalled(lua_State *L, int objIdx);

inline void luajavaNewJavaClassMetatable(lua_State *L);

inline void luajavaNewJavaObjectMetatable(lua_State *L);

inline void luajavaNewJavaArrayMetatable(lua_State *L);

#endif // LUAJAVA_LUAJAVA_API_H