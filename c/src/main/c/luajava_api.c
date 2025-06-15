#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <lauxlib.h>
#include "luajava_api.h"
#include "compatible.h"

/**
 * Generate lua stack.
 */
#define GENERATE_LUA_STATE_STACK( L, RESULT, MSG, ... ) \
  lua_Debug ar; \
  int level = 0; \
  snprintf(RESULT + strlen(RESULT), sizeof(RESULT) - strlen(RESULT), MSG, ##__VA_ARGS__); \
  while (lua_getstack(L, level++, &ar)) { \
    lua_getinfo(L, "nSl", &ar); \
    snprintf(RESULT, sizeof(RESULT), "%s\n\tat [LuaVM] [%d] [%s] %s: %s (%s:%d)", RESULT, \
            level - 1, ar.what ? ar.what : "(unknown what)", \
            ar.namewhat ? ar.namewhat : "(unknown namewhat)", \
            ar.name ? ar.name : "(unknown name)", \
            ar.source ? ar.source : "(unknown source)", ar.currentline); \
  }

/**
 * Throw lua error.
 */
#define THROW_LUA_ERROR( L, MSG, ... ) \
  char errorStack[1 << 10] = ""; \
  GENERATE_LUA_STATE_STACK(L, errorStack, MSG, ##__VA_ARGS__); \
  lua_pushstring(L, errorStack); \
  lua_error(L);

/**
 * @brief bind java class to target variable.
 * @arg TEMP_CLASS_VAR   - A temp variable.
 * @arg ENV              - JNIEnv.
 * @arg TARGET_CLASS_VAR - store result to target variable.
 * @arg CLASS_NAME       - class name, like "java/lang/String"
 */
#define BIND_JAVA_CLASS( TEMP_CLASS_VAR, ENV, TARGET_CLASS_VAR, CLASS_NAME ) \
  if (TARGET_CLASS_VAR == NULL) { \
    TEMP_CLASS_VAR = (*ENV)->FindClass(ENV, CLASS_NAME); \
    if (TEMP_CLASS_VAR == NULL) { \
      fprintf(stderr, "Could not find class: " CLASS_NAME "!\n"); \
      exit(1); \
    } \
    if ((TARGET_CLASS_VAR = (*ENV)->NewGlobalRef(ENV, TEMP_CLASS_VAR)) == NULL) { \
      fprintf(stderr, "Could not bind to class: " CLASS_NAME "!\n"); \
      exit(1); \
    } \
  }

/**
 * @brief bind java static method to target variable.
 * @arg ENV               - JNIEnv.
 * @arg TARGET_METHOD_VAR - store result to target variable.
 * @arg CLASS             - java class instance
 * @arg METHOD_NAME       - method name
 * @arg METHOD_SIGN       - like "()I"
 */
#define BIND_JAVA_STATIC_METHOD( ENV, TARGET_METHOD_VAR, CLASS, METHOD_NAME, METHOD_SIGN ) \
  BIND_JAVA_METHOD(ENV, GetStaticMethodID, TARGET_METHOD_VAR, CLASS, METHOD_NAME, METHOD_SIGN)

/**
 * @brief bind java normal method to target variable.
 * @arg ENV               - JNIEnv.
 * @arg TARGET_METHOD_VAR - store result to target variable.
 * @arg CLASS             - java class instance
 * @arg METHOD_NAME       - method name
 * @arg METHOD_SIGN       - like "()I"
 */
#define BIND_JAVA_NORMAL_METHOD( ENV, TARGET_METHOD_VAR, CLASS, METHOD_NAME, METHOD_SIGN ) \
  BIND_JAVA_METHOD(ENV, GetMethodID, TARGET_METHOD_VAR, CLASS, METHOD_NAME, METHOD_SIGN)

/**
 * @brief bind java method to target variable.
 * @arg ENV               - JNIEnv.
 * @arg FIND_METHOD       - JNIEnv method
 * @arg TARGET_METHOD_VAR - store result to target variable.
 * @arg CLASS             - java class instance
 * @arg METHOD_NAME       - method name
 * @arg METHOD_SIGN       - like "()I"
 */
#define BIND_JAVA_METHOD( ENV, FIND_METHOD, TARGET_METHOD_VAR, CLASS, METHOD_NAME, METHOD_SIGN ) \
  if (TARGET_METHOD_VAR == NULL) { \
    TARGET_METHOD_VAR = (*ENV)->FIND_METHOD(ENV, CLASS, METHOD_NAME, METHOD_SIGN); \
    if (!TARGET_METHOD_VAR) { \
      fprintf(stderr, "Could not find method \"" \
                       METHOD_NAME "( " METHOD_SIGN " )\"" \
                       " in class: "#CLASS ".\n"); \
      exit(1); \
    } \
  }

/**
 * @brief bind java field to target variable.
 * @arg ENV               - JNIEnv.
 * @arg TARGET_FIELD_VAR  - store result to target variable.
 * @arg CLASS             - java class instance
 * @arg FIELD_NAME        - field name
 * @arg FIELD_TYPE        - field type like "J"
 */
#define BIND_JAVA_NORMAL_FIELD( ENV, TARGET_FIELD_VAR, CLASS, FIELD_NAME, FIELD_TYPE ) \
  BIND_JAVA_FIELD( ENV, GetFieldID, TARGET_FIELD_VAR, CLASS, FIELD_NAME, FIELD_TYPE )

/**
 * @brief bind java field to target variable.
 * @arg ENV               - JNIEnv.
 * @arg FIND_METHOD       - JNIEnv method
 * @arg TARGET_FIELD_VAR  - store result to target variable.
 * @arg CLASS             - java class instance
 * @arg FIELD_NAME        - field name
 * @arg FIELD_TYPE        - field type like "J"
 */
#define BIND_JAVA_FIELD( ENV, FIND_METHOD, TARGET_FIELD_VAR, CLASS, FIELD_NAME, FIELD_TYPE ) \
  if (TARGET_FIELD_VAR == NULL) { \
    TARGET_FIELD_VAR = (*ENV)->FIND_METHOD(ENV, CLASS, FIELD_NAME, FIELD_TYPE); \
    if (!TARGET_FIELD_VAR) { \
      fprintf(stderr, "Could not find field \"" \
                       FIELD_NAME "( " FIELD_TYPE " )\"" \
                       " in class: "#CLASS ".\n"); \
      exit(1); \
    } \
  }

/**
 * @brief Handles exception
 * @arg L         - luaState*
 * @arg exp       - exception instance
 * @arg javaEnv   - JNIEnv
 * @arg CLEANCODE - Other code.
 */
#define HANDLES_JAVA_EXCEPTION( L, exp, javaEnv, CLEANCODE ) \
  if ( exp != NULL ) { \
    jobject jStr; \
    const char *cStr; \
    \
    (*javaEnv)->ExceptionClear(javaEnv); \
    jStr = (*javaEnv)->CallObjectMethod(javaEnv, exp, get_message_method); \
    \
    CLEANCODE \
    \
    if (jStr == NULL) { \
      jStr = (*javaEnv)->CallObjectMethod(javaEnv, exp, java_object_method_toString); \
    } \
    \
    cStr = (*javaEnv)->GetStringUTFChars(javaEnv, jStr, NULL); \
    \
    char errorStack[1 << 10] = "[C Side Exception] "; \
    strncat(errorStack, cStr, 100); \
    GENERATE_LUA_STATE_STACK(L, errorStack, ""); \
    lua_pushstring(L, errorStack); \
    \
    (*javaEnv)->ReleaseStringUTFChars(javaEnv, jStr, cStr); \
    lua_error(L); \
  }

static jclass    java_object_class           = NULL;
static jmethodID java_object_method_toString = NULL;

static jclass    throwable_class         = NULL;
static jmethodID get_message_method      = NULL;

static jclass    java_function_class     = NULL;
static jmethodID java_function_method    = NULL;
static jclass    java_lang_class         = NULL;
static jmethodID java_lang_class_static_method_forName   = NULL;

static jclass    luajava_api_class       = NULL;
static jmethodID luajava_api_static_method_checkField       = NULL;
static jmethodID luajava_api_static_method_checkMethod      = NULL;
static jmethodID luajava_api_static_method_objectIndex      = NULL;
static jmethodID luajava_api_static_method_classIndex       = NULL;
static jmethodID luajava_api_static_method_arrayIndex       = NULL;
static jmethodID luajava_api_static_method_arrayNewIndex    = NULL;
static jmethodID luajava_api_static_method_objectConcat     = NULL;
static jmethodID luajava_api_static_method_objectNewIndex   = NULL;
static jmethodID luajava_api_static_method_javaNew          = NULL;
static jmethodID luajava_api_static_method_javaNewInstance  = NULL;
static jmethodID luajava_api_static_method_javaLoadLib      = NULL;

static jclass    cptr_class = NULL;
static jfieldID  cptr_field_peer = NULL;

static unsigned char setup_luajava_api_result = 0;
/********************* Implementations ***************************/

/***************************************************************************
 *
 *  Function: setupLuaJavaApi
 *  ****/

void setupLuaJavaApi(JNIEnv *env) {
  setup_luajava_api_result = 1;

  jclass tempClass;
  
  BIND_JAVA_CLASS(tempClass, env, java_object_class, "java/lang/Object");
  BIND_JAVA_NORMAL_METHOD(env, java_object_method_toString, java_object_class, 
                          "toString", "()Ljava/lang/String;");

  BIND_JAVA_CLASS(tempClass, env, luajava_api_class, "org/keplerproject/luajava/LuaJavaAPI");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_checkField, luajava_api_class, 
                          "checkField", "(ILjava/lang/Object;Ljava/lang/String;)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_checkMethod, luajava_api_class, 
                          "checkMethod", "(ILjava/lang/Object;Ljava/lang/String;)Z");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_objectIndex, luajava_api_class, 
                          "objectIndex", "(ILjava/lang/Object;Ljava/lang/String;)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_classIndex, luajava_api_class, 
                          "classIndex", "(ILjava/lang/Class;Ljava/lang/String;)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_arrayIndex, luajava_api_class, 
                          "arrayIndex", "(ILjava/lang/Object;I)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_arrayNewIndex, luajava_api_class, 
                          "arrayNewIndex", "(ILjava/lang/Object;I)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_objectConcat, luajava_api_class, 
                          "objectConcat", "(I)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_objectNewIndex, luajava_api_class, 
                          "objectNewIndex", "(ILjava/lang/Object;Ljava/lang/String;)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_javaNew, luajava_api_class, 
                          "javaNew", "(ILjava/lang/Class;)I");                
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_javaNewInstance, luajava_api_class, 
                          "javaNewInstance", "(ILjava/lang/String;)I");     
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_javaLoadLib, luajava_api_class, 
                          "javaLoadLib", "(ILjava/lang/String;Ljava/lang/String;)I");   

  BIND_JAVA_CLASS(tempClass, env, java_function_class, "org/keplerproject/luajava/JavaFunction");
  BIND_JAVA_NORMAL_METHOD(env, java_function_method, java_function_class, 
                          "execute", "()I");

  BIND_JAVA_CLASS(tempClass, env, throwable_class, "java/lang/Throwable");
  BIND_JAVA_NORMAL_METHOD(env, get_message_method, throwable_class, 
                          "getMessage", "()Ljava/lang/String;");

  BIND_JAVA_CLASS(tempClass, env, java_lang_class, "java/lang/Class");
  BIND_JAVA_STATIC_METHOD(env, java_lang_class_static_method_forName, java_lang_class, 
                          "forName", "(Ljava/lang/String;)Ljava/lang/Class;");

  BIND_JAVA_CLASS(tempClass, env, cptr_class, "org/keplerproject/luajava/CPtr");
  BIND_JAVA_NORMAL_FIELD(env, cptr_field_peer, cptr_class, "peer", "J");
}

/***************************************************************************
 *
 *  Function: newCPtr
 *  ****/

jobject newCPtr(JNIEnv *env, jlong peer)
{
  if (!setup_luajava_api_result) setupLuaJavaApi(env);
  jobject obj = (*env)->AllocObject(env, cptr_class);
  if (obj) {
    (*env)->SetLongField(env, obj, cptr_field_peer, peer);
  }
  return obj;
}

/***************************************************************************
 *
 *  Function: getLuaStateIndex
 *  ****/

lua_Number getLuaStateIndex(lua_State *L) {
  lua_Number stateIndex;
  /* Gets the luaState index */
  lua_pushstring(L, LUAJAVASTATEINDEX);
  lua_rawget(L, LUA_REGISTRYINDEX);

  if (!lua_isnumber(L, -1)) {
     THROW_LUA_ERROR(L, "Impossible to identify luaState id.");
  }

  stateIndex = lua_tonumber(L, -1);
  lua_pop(L, 1);
  return stateIndex;
}

/***************************************************************************
 *
 *  Function: generateLuaStateStack
 *  ****/

void generateLuaStateStack(lua_State *L, char *stack_str) {
  lua_Debug ar;
  int level = 0;
  while (lua_getstack(L, level++, &ar)) {
    lua_getinfo(L, "nSl", &ar);
    snprintf(stack_str, 0, "%s\n\tat [LuaVM] [%d] [%s] %s: %s (%s:%d)", stack_str,
            level - 1, ar.what ? ar.what : "(unknown what)",
            ar.namewhat ? ar.namewhat : "(unknown namewhat)",
            ar.name ? ar.name : "(unknown name)",
            ar.source ? ar.source : "(unknown source)", ar.currentline);
  }
}

/***************************************************************************
 *
 *  Function: objectIndex
 *  ****/

int objectIndex(lua_State *L) {
  lua_Number stateIndex;
  const char *key;
  jint checkField;
  jboolean checkMethodRet;
  jobject *obj;
  jstring str;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid Java Object.");
  }

  if (!lua_isstring(L, -1)) {
    THROW_LUA_ERROR(L, "Invalid object index. Must be string.");
  }

  key = lua_tostring(L, -1);

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  obj = (jobject *)lua_touserdata(L, 1);

  str = (*javaEnv)->NewStringUTF(javaEnv, key);

  checkField = (*javaEnv)->CallStaticIntMethod(
      javaEnv, luajava_api_class, luajava_api_static_method_checkField, (jint)stateIndex, *obj, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  if (checkField != 0) {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
    return checkField;
  }

  checkMethodRet = (*javaEnv)->CallStaticBooleanMethod(
      javaEnv, luajava_api_class, luajava_api_static_method_checkMethod, (jint)stateIndex, *obj, str);
  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, str);
  if (!checkMethodRet) {
    return 0;
  }

  lua_getmetatable(L, 1);

  if (!lua_istable(L, -1)) {
     THROW_LUA_ERROR(L, "Invalid MetaTable.");
  }

  lua_pushstring(L, LUAJAVAOBJFUNCCALLED);
  lua_pushstring(L, key);
  lua_rawset(L, -3);

  lua_pop(L, 1);

  lua_pushcfunction(L, &objectIndexReturn);

  return 1;
}

/***************************************************************************
 *
 *  Function: objectIndexReturn
 *  ****/

int objectIndexReturn(lua_State *L) {
  lua_Number stateIndex;
  jobject *pObject;
  jthrowable exp;
  const char *methodName;
  jint ret;
  jstring str;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  /* Checks if is a valid java object */
  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid OO function call. "
     "If you want call a java method, use 'objet:method()' not 'object.method()'");
  }

  lua_getmetatable(L, 1);
  if (lua_type(L, -1) == LUA_TNIL) {
     THROW_LUA_ERROR(L, "Not a valid java Object.");
  }

  /* Gets the method Name */
  lua_pushstring(L, LUAJAVAOBJFUNCCALLED);
  lua_rawget(L, -2);
  if (lua_type(L, -1) == LUA_TNIL) {
     THROW_LUA_ERROR(L, "Not a OO function call."
     "If you want call a java method, use 'objet:method()' not 'object.method()'");
  }
  methodName = lua_tostring(L, -1);

  lua_pop(L, 2);

  /* Gets the object reference */
  pObject = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  /* Gets method */
  str = (*javaEnv)->NewStringUTF(javaEnv, methodName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_objectIndex,
                                        (jint)stateIndex, *pObject, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, str);

  /* pushes new object into lua stack */
  return ret;
}

/***************************************************************************
 *
 *  Function: objectNewIndex
 *  ****/

int objectNewIndex(lua_State *L) {
  lua_Number stateIndex;
  jobject *obj;
  const char *fieldName;
  jstring str;
  jint ret;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid java class.");
  }

  /* Gets the field Name */

  if (!lua_isstring(L, 2)) {
     THROW_LUA_ERROR(L, "Not a valid field call.");
  }

  fieldName = lua_tostring(L, 2);

  /* Gets the object reference */
  obj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  str = (*javaEnv)->NewStringUTF(javaEnv, fieldName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_objectNewIndex,
                                        (jint)stateIndex, *obj, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, str);

  return ret;
}

/***************************************************************************
 *
 *  Function: classIndex
 *  ****/

int classIndex(lua_State *L) {
  lua_Number stateIndex;
  jobject *obj;
  const char *fieldName;
  jstring str;
  jint ret;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid java class.");
  }

  /* Gets the field Name */

  if (!lua_isstring(L, 2)) {
     THROW_LUA_ERROR(L, "Not a valid field call.");
  }

  fieldName = lua_tostring(L, 2);

  /* Gets the object reference */
  obj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  str = (*javaEnv)->NewStringUTF(javaEnv, fieldName);

  /* Return 1 for field, 2 for method or 0 for error */
  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_classIndex,
                                        (jint)stateIndex, *obj, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, str);

  if (ret == 0) {
     THROW_LUA_ERROR(L, "Name is not a static field or function.");
  }

  if (ret == 2) {
    lua_getmetatable(L, 1);
    lua_pushstring(L, LUAJAVAOBJFUNCCALLED);
    lua_pushstring(L, fieldName);
    lua_rawset(L, -3);

    lua_pop(L, 1);

    lua_pushcfunction(L, &objectIndexReturn);

    return 1;
  }

  return ret;
}

/***************************************************************************
 *
 *  Function: arrayIndex
 *  ****/

int arrayIndex(lua_State *L) {
  lua_Number stateIndex;
  lua_Integer key;
  jint ret;
  jobject *obj;
  jthrowable exp;
  JNIEnv *javaEnv;

  /* Can index as number or string */
  if (!lua_isnumber(L, -1) && !lua_isstring(L, -1)) {
     THROW_LUA_ERROR(L, "Invalid object index. Must be integer or string.");
  }

  /* Important! If the index is not a number, behave as normal Java object */
  if (!lua_isnumber(L, -1)) {
    return objectIndex(L);
  }

  /* Index is number */

  stateIndex = getLuaStateIndex(L);

  // Array index
  key = lua_tointeger(L, -1);

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid Java Object.");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  obj = (jobject *)lua_touserdata(L, 1);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_arrayIndex,
                                        (jint)stateIndex, *obj, (jlong)key);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {});

  return ret;
}

/***************************************************************************
 *
 *  Function: arrayNewIndex
 *  ****/

int arrayNewIndex(lua_State *L) {
  lua_Number stateIndex;
  jobject *obj;
  lua_Integer key;
  jint ret;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid java class.");
  }

  /* Gets the field Name */

  if (!lua_isnumber(L, 2)) {
     THROW_LUA_ERROR(L, "Not a valid array index.");
  }

  key = lua_tointeger(L, 2);

  /* Gets the object reference */
  obj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_arrayNewIndex,
                                        (jint)stateIndex, *obj, (jint)key);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {});

  return ret;
}

/***************************************************************************
 *
 *  Function: arrayLength
 *  ****/

int arrayLength(lua_State *L) {
  jobject *obj;
  JNIEnv *javaEnv;
  jsize len;

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid Java Object!");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  obj = (jobject *)lua_touserdata(L, 1);
  len = (*javaEnv)->GetArrayLength(javaEnv, *obj);
  lua_pushinteger(L, len);
  return 1;
}

/***************************************************************************
 *
 *  Function: objectContainerSize
 *  ****/

int objectContainerSize(lua_State *L) {
  jmethodID method;
  jobject *obj;
  JNIEnv *javaEnv;
  jthrowable exp;
  jint ret;
  jclass clazz;
  jboolean flag;

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a valid Java Object!");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  obj = (jobject *)lua_touserdata(L, 1);
  clazz = (*javaEnv)->FindClass(javaEnv, "java/util/Collection");
  if (clazz != NULL) {
    flag = (*javaEnv)->IsInstanceOf(javaEnv, *obj, clazz);
  }
  if (!flag) {
    clazz = (*javaEnv)->FindClass(javaEnv, "java/util/Map");
    if (clazz != NULL) {
      flag = (*javaEnv)->IsInstanceOf(javaEnv, *obj, clazz);
    }
  }
  if (!flag) {
     THROW_LUA_ERROR(L, "This object is not a instance of Map or Collection.");
  }

  method = (*javaEnv)->GetMethodID(javaEnv, clazz, "size", "()I");
  if (method == NULL) {
     THROW_LUA_ERROR(
        L, "Cannot get the object's size, because not found size method.");
  }
  ret = (*javaEnv)->CallIntMethod(javaEnv, *obj, method);
  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {});

  lua_pushinteger(L, ret);
  return 1;
}

/***************************************************************************
 *
 *  Function: javaObjectEquals
 *  ****/

int javaObjectEquals(lua_State *L) {
  jobject *objA;
  jobject *objB;
  JNIEnv *javaEnv;
  jboolean ret;

  if (!isJavaObject(L, 1) || !isJavaObject(L, 2)) {
    lua_pushboolean(L, 0);
    return 1;
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  objA = (jobject *)lua_touserdata(L, 1);
  objB = (jobject *)lua_touserdata(L, 2);

  if (objA == objB) {
    lua_pushboolean(L, 1);
    return 1;
  }
  ret = (*javaEnv)->IsSameObject(javaEnv, *objA, *objB);
  lua_pushboolean(L, ret);
  return 1;
}

/***************************************************************************
 *
 *  Function: javaStringConcat
 *  ****/

int javaStringConcat(lua_State *L) {
  JNIEnv *javaEnv;
  lua_Number stateIndex;
  jthrowable exp;
  jint ret;

  if (!isJavaObject(L, 1) && !isJavaObject(L, 2)) {
     THROW_LUA_ERROR(
        L, "In the concat operation, at least one java object is required.");
  }

  stateIndex = getLuaStateIndex(L);

  // java env
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, 
                                        luajava_api_static_method_objectConcat, (jint)stateIndex);
  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {});
  
  return ret;
}

/***************************************************************************
 *
 *  Function: gc
 *  ****/

int gc(lua_State *L) {
  jobject *pObj;
  JNIEnv *javaEnv;

  if (!isJavaObject(L, 1)) {
    return 0;
  }

  pObj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  (*javaEnv)->DeleteGlobalRef(javaEnv, *pObj);

  return 0;
}

/***************************************************************************
 *
 *  Function: javaBindClass
 *  ****/

int javaBindClass(lua_State *L) {
  int top;
  const char *className;
  jstring javaClassName;
  jobject classInstance;
  jthrowable exp;
  JNIEnv *javaEnv;

  top = lua_gettop(L);

  if (top != 1) {
    luaL_error(
        L, "Error. Function javaBindClass received %d arguments, expected 1.",
        top);
  }

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  /* get the string parameter */
  if (!lua_isstring(L, 1)) {
     THROW_LUA_ERROR(L, "Invalid parameter type. String expected.");
  }
  className = lua_tostring(L, 1);

  javaClassName = (*javaEnv)->NewStringUTF(javaEnv, className);

  classInstance = (*javaEnv)->CallStaticObjectMethod(javaEnv, java_lang_class,
                                                     java_lang_class_static_method_forName, javaClassName);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, javaClassName);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, javaClassName);

  /* pushes new object into lua stack */

  return pushJavaClass(L, classInstance);
}

/***************************************************************************
 *
 *  Function: createProxy
 *  ****/
int createProxy(lua_State *L) {
  jint ret;
  lua_Number stateIndex;
  const char *impl;
  jmethodID method;
  jthrowable exp;
  jstring str;
  JNIEnv *javaEnv;

  if (lua_gettop(L) != 2) {
     THROW_LUA_ERROR(L, "Error. Function createProxy expects 2 arguments.");
  }

  stateIndex = getLuaStateIndex(L);

  if (!lua_isstring(L, 1) || !lua_istable(L, 2)) {
     THROW_LUA_ERROR(L, "Invalid Argument types. Expected (string, table).");
  }

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  method = (*javaEnv)->GetStaticMethodID(javaEnv, luajava_api_class,
                                         "createProxyObject",
                                         "(ILjava/lang/String;)I");

  impl = lua_tostring(L, 1);

  str = (*javaEnv)->NewStringUTF(javaEnv, impl);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
                                        (jint)stateIndex, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, str);

  return ret;
}

/***************************************************************************
 *
 *  Function: javaNew
 *  ****/

int javaNew(lua_State *L) {
  int top;
  jint ret;
  jobject classInstance;
  jthrowable exp;
  jobject *userData;
  lua_Number stateIndex;
  JNIEnv *javaEnv;

  top = lua_gettop(L);

  if (top == 0) {
     THROW_LUA_ERROR(L, "Error. Invalid number of parameters.");
  }

  stateIndex = getLuaStateIndex(L);

  /* Gets the java Class reference */
  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Argument not a valid Java Class.");
  }

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  userData = (jobject *)lua_touserdata(L, 1);

  classInstance = (jobject)*userData;

  if ((*javaEnv)->IsInstanceOf(javaEnv, classInstance, java_lang_class) == JNI_FALSE) {
     THROW_LUA_ERROR(L, "Argument not a valid Java Class.");
  }

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_javaNew,
                                        (jint)stateIndex, classInstance);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {});

  return ret;
}

/***************************************************************************
 *
 *  Function: javaNewInstance
 *  ****/

int javaNewInstance(lua_State *L) {
  jint ret;
  const char *className;
  jstring javaClassName;
  jthrowable exp;
  lua_Number stateIndex;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  /* get the string parameter */
  if (!lua_isstring(L, 1)) {
     THROW_LUA_ERROR(
        L, "Invalid parameter type. String expected as first parameter.");
  }

  className = lua_tostring(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  javaClassName = (*javaEnv)->NewStringUTF(javaEnv, className);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_javaNewInstance,
                                        (jint)stateIndex, javaClassName);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);  
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, javaClassName);
  });
  (*javaEnv)->DeleteLocalRef(javaEnv, javaClassName);

  return ret;
}

/***************************************************************************
 *
 *  Function: javaLoadLib
 *  ****/

int javaLoadLib(lua_State *L) {
  jint ret;
  int top;
  const char *className, *methodName;
  lua_Number stateIndex;
  jthrowable exp;
  jstring javaClassName, javaMethodName;
  JNIEnv *javaEnv;

  top = lua_gettop(L);

  if (top != 2) {
     THROW_LUA_ERROR(L, "Error. Invalid number of parameters.");
  }

  stateIndex = getLuaStateIndex(L);

  if (!lua_isstring(L, 1) || !lua_isstring(L, 2)) {
     THROW_LUA_ERROR(L, "Invalid parameter. Strings expected.");
  }

  className = lua_tostring(L, 1);
  methodName = lua_tostring(L, 2);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  javaClassName = (*javaEnv)->NewStringUTF(javaEnv, className);
  javaMethodName = (*javaEnv)->NewStringUTF(javaEnv, methodName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_javaLoadLib,
                                        (jint)stateIndex, javaClassName, javaMethodName);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, javaClassName);
    (*javaEnv)->DeleteLocalRef(javaEnv, javaMethodName);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, javaClassName);
  (*javaEnv)->DeleteLocalRef(javaEnv, javaMethodName);

  return ret;
}

/***************************************************************************
 *
 *  Function: pushJavaClass
 *  ****/

int pushJavaClass(lua_State *L, jobject javaObject) {
  jobject *userData, globalRef;

  /* Gets the JNI Environment */
  JNIEnv *javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  globalRef = (*javaEnv)->NewGlobalRef(javaEnv, javaObject);

  userData = (jobject *)lua_newuserdata(L, sizeof(jobject));
  *userData = globalRef;

  /* Creates metatable */
  lua_newtable(L);

  /* pushes the __index metamethod */
  lua_pushstring(L, LUAINDEXMETAMETHODTAG);
  lua_pushcfunction(L, &classIndex);
  lua_rawset(L, -3);

  /* pushes the __newindex metamethod */
  lua_pushstring(L, LUANEWINDEXMETAMETHODTAG);
  lua_pushcfunction(L, &objectNewIndex);
  lua_rawset(L, -3);

  /* pushes the __eq metamethod */
  lua_pushstring(L, LUA_EQ_METAMETHOD_TAG);
  lua_pushcfunction(L, &javaObjectEquals);
  lua_rawset(L, -3);

  /* pushes the __concat metamethod */
  lua_pushstring(L, LUA_CONCAT_METAMETHOD_TAG);
  lua_pushcfunction(L, &javaStringConcat);
  lua_rawset(L, -3);

  /* pushes the __gc metamethod */
  lua_pushstring(L, LUAGCMETAMETHODTAG);
  lua_pushcfunction(L, &gc);
  lua_rawset(L, -3);

  /* Is Java Object boolean */
  lua_pushstring(L, LUAJAVAOBJECTIND);
  lua_pushboolean(L, 1);
  lua_rawset(L, -3);

  if (lua_setmetatable(L, -2) == 0) {
    (*javaEnv)->DeleteGlobalRef(javaEnv, globalRef);
     THROW_LUA_ERROR(L, "Cannot create proxy to java class.");
  }

  return 1;
}

/***************************************************************************
 *
 *  Function: pushJavaObject
 *  ****/

int pushJavaObject(lua_State *L, jobject javaObject) {
  jobject *userData, globalRef;

  /* Gets the JNI Environment */
  JNIEnv *javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  globalRef = (*javaEnv)->NewGlobalRef(javaEnv, javaObject);

  userData = (jobject *)lua_newuserdata(L, sizeof(jobject));
  *userData = globalRef;

  /* Creates metatable */
  lua_newtable(L);

  /* pushes the __index metamethod */
  lua_pushstring(L, LUAINDEXMETAMETHODTAG);
  lua_pushcfunction(L, &objectIndex);
  lua_rawset(L, -3);

  /* pushes the __newindex metamethod */
  lua_pushstring(L, LUANEWINDEXMETAMETHODTAG);
  lua_pushcfunction(L, &objectNewIndex);
  lua_rawset(L, -3);

  /* pushes the __eq metamethod */
  lua_pushstring(L, LUA_EQ_METAMETHOD_TAG);
  lua_pushcfunction(L, &javaObjectEquals);
  lua_rawset(L, -3);

  /* pushes the __concat metamethod */
  lua_pushstring(L, LUA_CONCAT_METAMETHOD_TAG);
  lua_pushcfunction(L, &javaStringConcat);
  lua_rawset(L, -3);

  /* pushes the __len metamethod */
  lua_pushstring(L, LUA_LEN_METAMETHOD_TAG);
  lua_pushcfunction(L, &objectContainerSize);
  lua_rawset(L, -3);

  /* pushes the __gc metamethod */
  lua_pushstring(L, LUAGCMETAMETHODTAG);
  lua_pushcfunction(L, &gc);
  lua_rawset(L, -3);

  /* Is Java Object boolean */
  lua_pushstring(L, LUAJAVAOBJECTIND);
  lua_pushboolean(L, 1);
  lua_rawset(L, -3);

  if (lua_setmetatable(L, -2) == 0) {
    (*javaEnv)->DeleteGlobalRef(javaEnv, globalRef);
     THROW_LUA_ERROR(L, "Cannot create proxy to java object.");
  }

  return 1;
}

/***************************************************************************
 *
 *  Function: pushJavaArray
 *  ****/

int pushJavaArray(lua_State *L, jobject javaObject) {
  jobject *userData, globalRef;

  /* Gets the JNI Environment */
  JNIEnv *javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  globalRef = (*javaEnv)->NewGlobalRef(javaEnv, javaObject);

  userData = (jobject *)lua_newuserdata(L, sizeof(jobject));
  *userData = globalRef;

  /* Creates metatable */
  lua_newtable(L);

  /* pushes the __index metamethod */
  lua_pushstring(L, LUAINDEXMETAMETHODTAG);
  lua_pushcfunction(L, &arrayIndex);
  lua_rawset(L, -3);

  /* pushes the __newindex metamethod */
  lua_pushstring(L, LUANEWINDEXMETAMETHODTAG);
  lua_pushcfunction(L, &arrayNewIndex);
  lua_rawset(L, -3);

  /* pushes the __eq metamethod */
  lua_pushstring(L, LUA_EQ_METAMETHOD_TAG);
  lua_pushcfunction(L, &javaObjectEquals);
  lua_rawset(L, -3);

  /* pushes the __concat metamethod */
  lua_pushstring(L, LUA_CONCAT_METAMETHOD_TAG);
  lua_pushcfunction(L, &javaStringConcat);
  lua_rawset(L, -3);

  /* pushes the __len metamethod */
  lua_pushstring(L, LUA_LEN_METAMETHOD_TAG);
  lua_pushcfunction(L, &arrayLength);
  lua_rawset(L, -3);

  /* pushes the __gc metamethod */
  lua_pushstring(L, LUAGCMETAMETHODTAG);
  lua_pushcfunction(L, &gc);
  lua_rawset(L, -3);

  /* Is Java Object boolean */
  lua_pushstring(L, LUAJAVAOBJECTIND);
  lua_pushboolean(L, 1);
  lua_rawset(L, -3);

  if (lua_setmetatable(L, -2) == 0) {
    (*javaEnv)->DeleteGlobalRef(javaEnv, globalRef);
     THROW_LUA_ERROR(L, "Cannot create proxy to java object.");
  }

  return 1;
}

/***************************************************************************
 *
 *  Function: isJavaObject
 *  ****/

int isJavaObject(lua_State *L, int idx) {
  if (!lua_isuserdata(L, idx))
    return 0;

  if (lua_getmetatable(L, idx) == 0)
    return 0;

  lua_pushstring(L, LUAJAVAOBJECTIND);
  lua_rawget(L, -2);

  if (lua_isnil(L, -1)) {
    lua_pop(L, 2);
    return 0;
  }
  lua_pop(L, 2);
  return 1;
}

/***************************************************************************
 *
 *  Function: isJavaFunctionInstance
 *  ****/

jboolean isJavaFunctionInstance(JNIEnv *env, jobject *obj) {
  return (*env)->IsInstanceOf(env, *obj, java_function_class);
}

/***************************************************************************
 *
 *  Function: getStateFromCPtr
 *  ****/

lua_State *getStateFromCPtr(JNIEnv *env, jobject cptr) {
  lua_State *L;
  jbyte *peer = (jbyte *)(*env)->GetLongField(env, cptr, cptr_field_peer);
  L = (lua_State *)peer;
  pushJNIEnv(env, L);
  return L;
}

/***************************************************************************
 *
 *  Function: resetCPtr
 *  ****/

void resetCPtr(JNIEnv *env, jobject cptr) {
  (*env)->SetLongField(env, cptr, cptr_field_peer, (jlong)0);
}

/***************************************************************************
 *
 *  Function: luaJavaFunctionCall
 *  ****/

int luaJavaFunctionCall(lua_State *L) {
  jobject *obj;
  jthrowable exp;
  int ret;
  JNIEnv *javaEnv;

  if (!isJavaObject(L, 1)) {
     THROW_LUA_ERROR(L, "Not a java Function.");
  }

  obj = lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
     THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  /* the Object must be an instance of the JavaFunction class */
  if ((*javaEnv)->IsInstanceOf(javaEnv, *obj, java_function_class) ==
      JNI_FALSE) {
    fprintf(stderr, "Called Java object is not a JavaFunction\n");
    return 0;
  }

  ret = (*javaEnv)->CallIntMethod(javaEnv, *obj, java_function_method);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {});

  return ret;
}

/***************************************************************************
 *
 *  Function: luaJavaFunctionCall
 *  ****/

JNIEnv *getEnvFromState(lua_State *L) {
  JNIEnv **udEnv;

  lua_pushstring(L, LUAJAVAJNIENVTAG);
  lua_rawget(L, LUA_REGISTRYINDEX);

  if (!lua_isuserdata(L, -1)) {
    lua_pop(L, 1);
    return NULL;
  }

  udEnv = (JNIEnv **)lua_touserdata(L, -1);

  lua_pop(L, 1);

  return *udEnv;
}

/***************************************************************************
 *
 *  Function: pushJNIEnv
 *  ****/

void pushJNIEnv(JNIEnv *env, lua_State *L) {
  JNIEnv **udEnv;

  lua_pushstring(L, LUAJAVAJNIENVTAG);
  lua_rawget(L, LUA_REGISTRYINDEX);

  if (!lua_isnil(L, -1)) {
    udEnv = (JNIEnv **)lua_touserdata(L, -1);
    *udEnv = env;
    lua_pop(L, 1);
  } else {
    lua_pop(L, 1);
    udEnv = (JNIEnv **)lua_newuserdata(L, sizeof(JNIEnv *));
    *udEnv = env;

    lua_pushstring(L, LUAJAVAJNIENVTAG);
    lua_insert(L, -2);
    lua_rawset(L, LUA_REGISTRYINDEX);
  }
}