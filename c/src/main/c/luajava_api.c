/******************************************************************************
 * luajava_api.c, SmileYik, 2025-8-10
 * Copyright (c) 2025 Smile Yik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

/******************************************************************************
 * $Id$
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

#include <lua.h>
#include <luaconf.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <lauxlib.h>
#include "luajava_api.h"
#include "compatible.h"
#include "hashmap.h"

#ifdef DEBUG_FLAG
  #define DEBUGF(STR, ...) printf(STR, __VA_ARGS__); fflush(stdout)
  #define DEBUG(STR) printf(STR); fflush(stdout)
#else
  #define DEBUGF(STR, ...)
  #define DEBUG(STR)
#endif

/**
 * Generate lua stack.
 */
#define GENERATE_LUA_STATE_STACK( L, RESULT, MSG, ... ) \
  lua_Debug ar; \
  int level = 0; \
  size_t len; \
  size_t size = sizeof(RESULT); \
  snprintf(RESULT + strlen(RESULT), sizeof(RESULT) - strlen(RESULT), MSG, ##__VA_ARGS__); \
  while (lua_getstack(L, level++, &ar)) { \
    lua_getinfo(L, "nSl", &ar); \
    len = strlen(RESULT); \
    if (size > len) { \
        snprintf(RESULT + len, size - len, "\n\tat [LuaVM] [%d] [%s] %s: %s (%s:%d)", \
                level - 1, ar.what ? ar.what : "(unknown what)", \
                ar.namewhat ? ar.namewhat : "(unknown namewhat)", \
                ar.name ? ar.name : "(unknown name)", \
                ar.source ? ar.source : "(unknown source)", ar.currentline); \
    } \
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
    char errorStack[1 << 16] = "[C Side Exception] "; \
    strncat(errorStack, cStr, sizeof(errorStack)); \
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
static jmethodID luajava_api_static_method_debugLuaHook     = NULL;
static jmethodID luajava_api_static_method_newLuaDebug      = NULL;
static jmethodID luajava_api_static_method_luaWrite         = NULL;
static jmethodID luajava_api_static_method_luaRead          = NULL;

static jclass    luajava_rw_entity_class                    = NULL;
static jmethodID luajava_rw_entity_method_bufferSize        = NULL;
static jmethodID luajava_rw_entity_method_setDataPtr        = NULL;
static jmethodID luajava_rw_entity_method_getDataPtr        = NULL;

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

  BIND_JAVA_CLASS(tempClass, env, luajava_api_class, "org/eu/smileyik/luajava/LuaJavaAPI");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_checkField, luajava_api_class, 
                          "checkField", "(ILjava/lang/Object;Ljava/lang/String;)I");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_checkMethod, luajava_api_class, 
                          "checkMethod", "(ILjava/lang/Object;Ljava/lang/String;)Z");
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_objectIndex, luajava_api_class, 
                          "objectIndex", "(ILjava/lang/Object;Ljava/lang/String;Z)I");
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
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_debugLuaHook, luajava_api_class, 
                          "debugLuaHook", "(ILorg/eu/smileyik/luajava/debug/LuaDebug;)V");   
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_newLuaDebug, luajava_api_class, 
                          "newLuaDebug", "(JLjava/nio/ByteBuffer;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lorg/eu/smileyik/luajava/debug/LuaDebug;");   
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_luaWrite, luajava_api_class, 
                          "luaWrite", "(ILjava/nio/ByteBuffer;Lorg/eu/smileyik/luajava/ILuaReadWriteEntity;)V");   
  BIND_JAVA_STATIC_METHOD(env, luajava_api_static_method_luaRead, luajava_api_class, 
                          "luaRead", "(ILjava/nio/ByteBuffer;Lorg/eu/smileyik/luajava/ILuaReadWriteEntity;)I"); 

  BIND_JAVA_CLASS(tempClass, env, java_function_class, "org/eu/smileyik/luajava/JavaFunction");
  BIND_JAVA_NORMAL_METHOD(env, java_function_method, java_function_class, 
                          "execute", "()I");

  BIND_JAVA_CLASS(tempClass, env, throwable_class, "java/lang/Throwable");
  BIND_JAVA_NORMAL_METHOD(env, get_message_method, throwable_class, 
                          "getMessage", "()Ljava/lang/String;");

  BIND_JAVA_CLASS(tempClass, env, java_lang_class, "java/lang/Class");
  BIND_JAVA_STATIC_METHOD(env, java_lang_class_static_method_forName, java_lang_class, 
                          "forName", "(Ljava/lang/String;)Ljava/lang/Class;");

  BIND_JAVA_CLASS(tempClass, env, cptr_class, "org/eu/smileyik/luajava/CPtr");
  BIND_JAVA_NORMAL_FIELD(env, cptr_field_peer, cptr_class, "peer", "J");

  BIND_JAVA_CLASS(tempClass, env, luajava_rw_entity_class, "org/eu/smileyik/luajava/ILuaReadWriteEntity");
  BIND_JAVA_NORMAL_METHOD(env, luajava_rw_entity_method_bufferSize, luajava_rw_entity_class,
                          "bufferSize", "()I");
  BIND_JAVA_NORMAL_METHOD(env, luajava_rw_entity_method_setDataPtr, luajava_rw_entity_class,
                          "setDataPtr", "(J)V");
  BIND_JAVA_NORMAL_METHOD(env, luajava_rw_entity_method_getDataPtr, luajava_rw_entity_class,
                          "getDataPtr", "()J");
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
    lua_State *L = (lua_State *) ((jbyte *) peer);

#ifdef LUAJAVA_FORCE_SAME_METATABLE_CLASS
    lua_pushstring(L, LUAJAVA_METATABLE_CLASS);
    luajavaNewJavaClassMetatable(L);
    lua_rawset(L, LUA_REGISTRYINDEX);
#endif

#ifdef LUAJAVA_FORCE_SAME_METATABLE_OBJECT
    lua_pushstring(L, LUAJAVA_METATABLE_OBJECT);
    luajavaNewJavaObjectMetatable(L);
    lua_rawset(L, LUA_REGISTRYINDEX);
#endif

#ifdef LUAJAVA_FORCE_SAME_METATABLE_ARRAY
    lua_pushstring(L, LUAJAVA_METATABLE_ARRAY);
    luajavaNewJavaArrayMetatable(L);
    lua_rawset(L, LUA_REGISTRYINDEX);
#endif
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

  lua_pushvalue(L, 1);
  lua_pushstring(L, key);
  lua_pushboolean(L, 0);
  lua_pushcclosure(L, &objectIndexReturn, 3);
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

  /* Gets the object reference */
  pObject = (jobject *)lua_touserdata(L, lua_upvalueindex(1));
  methodName = lua_tostring(L, lua_upvalueindex(2));

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }

  /* Gets method */
  str = (*javaEnv)->NewStringUTF(javaEnv, methodName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, luajava_api_static_method_objectIndex,
                                        (jint)stateIndex, *pObject, str, lua_toboolean(L, lua_upvalueindex(3)));

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
    THROW_LUA_ERROR(L, "Indexed name is not a static field or function.");
  }

  if (ret == 2) {
    lua_pushvalue(L, 1);
    lua_pushstring(L, fieldName);
    lua_pushboolean(L, 1);
    lua_pushcclosure(L, &objectIndexReturn, 3);
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
 *  Function: javaClass2Obj
 *  ****/

int javaClass2Obj(lua_State *L) {
  if (!isJavaObject(L, 1)) {
    THROW_LUA_ERROR(L, "Only a java class can be cast to java object instance");
  }

  // just change metatable

#ifdef LUAJAVA_FORCE_SAME_METATABLE_OBJECT
  lua_pushstring(L, LUAJAVA_METATABLE_OBJECT);
  lua_rawget(L, LUA_REGISTRYINDEX);
#else
  luajavaNewJavaObjectMetatable(L);
#endif

  if (lua_setmetatable(L, -2) == 0) {
    THROW_LUA_ERROR(L, "Cannot create proxy to java object.");
  }

  return 1;
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

#ifdef LUAJAVA_FORCE_SAME_METATABLE_CLASS
  lua_pushstring(L, LUAJAVA_METATABLE_CLASS);
  lua_rawget(L, LUA_REGISTRYINDEX);
#else
  luajavaNewJavaClassMetatable(L);
#endif

  if (lua_setmetatable(L, -2) == 0) {
    (*javaEnv)->DeleteGlobalRef(javaEnv, globalRef);
    THROW_LUA_ERROR(L, "Cannot create proxy to java class.");
  }

  return 1;
}

/***************************************************************************
 *
 *  Function: getJNIEnv
 *  ****/

int getJNIEnv(lua_State *L) {
  JNIEnv *javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }
  lua_pushlightuserdata(L, javaEnv);
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

#ifdef LUAJAVA_FORCE_SAME_METATABLE_OBJECT
  lua_pushstring(L, LUAJAVA_METATABLE_OBJECT);
  lua_rawget(L, LUA_REGISTRYINDEX);
#else
  luajavaNewJavaObjectMetatable(L);
#endif

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

#ifdef LUAJAVA_FORCE_SAME_METATABLE_ARRAY
  lua_pushstring(L, LUAJAVA_METATABLE_ARRAY);
  lua_rawget(L, LUA_REGISTRYINDEX);
#else
  luajavaNewJavaArrayMetatable(L);
#endif

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

int luajavaGetJavaObjectType(lua_State *L, int idx) {
  if (!lua_isuserdata(L, idx))
    return 0;

  if (lua_getmetatable(L, idx) == 0)
    return 0;

  lua_pushstring(L, LUAJAVA_OBJECT_TYPE);
  lua_rawget(L, -2);

  if (!lua_isnumber(L, -1)) {
    lua_pop(L, 2);
    return LUAJAVA_OBJECT_TYPE_NOT_JAVA_OBJECT;
  }
  int result = lua_tointeger(L, -1);
  lua_pop(L, 2);
  return result;
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

/***************************************************************************
 *
 *  Function: luajavaSetObjectFunctionCalled
 *  ****/

void luajavaSetObjectFunctionCalled(lua_State *L, int objIdx, const char *methodName) {
  LUAJAVA_GET_METATABLE(L, objIdx);

  if (!lua_istable(L, -1)) {
    lua_pop(L, 1);
    lua_newtable(L);
    lua_pushstring(L, LUAJAVAOBJFUNCCALLED);
    lua_pushstring(L, methodName);
    lua_rawset(L, -3);
    LUAJAVA_SET_METATABLE(L, objIdx);
  } else {
    lua_pushstring(L, LUAJAVAOBJFUNCCALLED);
    lua_pushstring(L, methodName);
    lua_rawset(L, -3);
    lua_pop(L, 1);
  }
}

/***************************************************************************
 *
 *  Function: luajavaGetObjectFunctionCalled
 *  ****/

const char* luajavaGetObjectFunctionCalled(lua_State *L, int objIdx) {
  const char* methodName;

  LUAJAVA_GET_METATABLE(L, objIdx);
  if (!lua_istable(L, -1)) {
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
  return methodName;
}

void luajavaNewJavaClassMetatable(lua_State *L) {
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

  /* Java Object Type integer */
  lua_pushstring(L, LUAJAVA_OBJECT_TYPE);
  lua_pushinteger(L, LUAJAVA_OBJECT_TYPE_CLASS);
  lua_rawset(L, -3);
}

void luajavaNewJavaObjectMetatable(lua_State *L) {
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

  /* Java Object Type integer */
  lua_pushstring(L, LUAJAVA_OBJECT_TYPE);
  lua_pushinteger(L, LUAJAVA_OBJECT_TYPE_OBJECT);
  lua_rawset(L, -3);
}

void luajavaNewJavaArrayMetatable(lua_State *L) {
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

  /* Java Object Type integer */
  lua_pushstring(L, LUAJAVA_OBJECT_TYPE);
  lua_pushinteger(L, LUAJAVA_OBJECT_TYPE_ARRAY);
  lua_rawset(L, -3);
}

/***************************************************************************
 *
 *  Function: luajavaLuaHook
 *  ****/

void luajavaLuaHook(lua_State *L, lua_Debug *ar) {
  JNIEnv *env = getEnvFromState(L);
  if (env == NULL) {
    THROW_LUA_ERROR(L, "Invalid JNI Environment.");
  }
  lua_Number stateIndex = getLuaStateIndex(L);
  lua_getinfo(L, "lSnu", ar);
  jobject luaDebug = luajavaNewLuaDebug(L, env, ar, "lSnu");
  (*env)->CallStaticVoidMethod(env, luajava_api_class, luajava_api_static_method_debugLuaHook,
                                (jint) stateIndex, luaDebug);
}

/***************************************************************************
 *
 *  Function: luajavaNewLuaDebug
 *  ****/

jobject luajavaNewLuaDebug(lua_State *L, JNIEnv *env, const lua_Debug *ar, const char* arWhat) {
  jstring name = NULL;
  jstring nameWhat = NULL;
  jstring what = NULL;
  jstring source = NULL;
  jstring shortSrc = NULL;
  char nameFlag = 0;
  char sourceFlag = 0;
  if (arWhat) {
    size_t whatLen = strlen(arWhat);
    size_t i = 0;
    for (; i < whatLen; ++i) {
      if (arWhat[i] == 'n') {
        nameFlag = 1;
      } else if (arWhat[i] == 'S') {
        sourceFlag = 1;
      }
    }
  }

  if (nameFlag) {
    name = (*env)->NewStringUTF(env, ar->name);
    nameWhat = (*env)->NewStringUTF(env, ar->namewhat);
  }
  if (sourceFlag) {
    what = (*env)->NewStringUTF(env, ar->what);
    source = (*env)->NewStringUTF(env, ar->source);
    shortSrc = (*env)->NewStringUTF(env, ar->short_src);
  }

  jobject byteBuffer = (*env)->NewDirectByteBuffer(env, ar, sizeof(lua_Debug));
  jobject result = (*env)->CallStaticObjectMethod(env, luajava_api_class, luajava_api_static_method_newLuaDebug,
                                                    (jlong) ar, byteBuffer, name, nameWhat, what, source);

  jthrowable exp = (*env)->ExceptionOccurred(env);  
  HANDLES_JAVA_EXCEPTION(L, exp, env, {
    if (name) (*env)->DeleteLocalRef(env, name);
    if (nameWhat) (*env)->DeleteLocalRef(env, nameWhat);
    if (what) (*env)->DeleteLocalRef(env, what);
    if (source) (*env)->DeleteLocalRef(env, source);
    if (shortSrc) (*env)->DeleteLocalRef(env, shortSrc);
    return NULL;
  });

  if (name) (*env)->DeleteLocalRef(env, name);
  if (nameWhat) (*env)->DeleteLocalRef(env, nameWhat);
  if (what) (*env)->DeleteLocalRef(env, what);
  if (source) (*env)->DeleteLocalRef(env, source);
  if (shortSrc) (*env)->DeleteLocalRef(env, shortSrc);

  return result;
}

/***************************************************************************
 *
 *  Function: luajavaLuaWriter
 *  ****/

int luajavaLuaWriter(lua_State *L, const void *p, size_t sz, void *ud) {
  // read from lua / write to userdata
  JNIEnv *env = getEnvFromState(L);
  lua_Number stateIdx = getLuaStateIndex(L);
  jobject byteBuffer = (*env)->NewDirectByteBuffer(env, p, sz);
  (*env)->CallStaticVoidMethod(env, luajava_api_class, luajava_api_static_method_luaWrite,
                                (jint) stateIdx, byteBuffer, (jobject) ud);
  jthrowable exp = (*env)->ExceptionOccurred(env);  
  HANDLES_JAVA_EXCEPTION(L, exp, env, {
    return 1;
  });
  return 0;
}

/***************************************************************************
 *
 *  Function: luajavaLuaReader
 *  ****/

const char* luajavaLuaReader(lua_State *L, void *ud, size_t *size) {
  // write into lua / read from userdata
  JNIEnv *env = getEnvFromState(L);
  lua_Number stateIdx = getLuaStateIndex(L);
  char* buffer = NULL;
  jint bufferSize = (*env)->CallIntMethod(env, (jobject) ud, luajava_rw_entity_method_bufferSize);

  jthrowable exp = (*env)->ExceptionOccurred(env);  
  HANDLES_JAVA_EXCEPTION(L, exp, env, {
    return NULL;
  });

  buffer = (char *) ((jbyte*) (*env)->CallLongMethod(env, (jobject) ud, luajava_rw_entity_method_getDataPtr));
  exp = (*env)->ExceptionOccurred(env);  
  HANDLES_JAVA_EXCEPTION(L, exp, env, {
    return NULL;
  });

  if (buffer == NULL) {
    buffer = (char *) malloc(sizeof(char) * (int) bufferSize);
    (*env)->CallVoidMethod(env, (jobject) ud, luajava_rw_entity_method_setDataPtr, (jlong) buffer);
    exp = (*env)->ExceptionOccurred(env);  
    HANDLES_JAVA_EXCEPTION(L, exp, env, {
      free(buffer);
      return NULL;
    });
  }

  jobject byteBuffer = (*env)->NewDirectByteBuffer(env, buffer, bufferSize);
  jint len = (*env)->CallStaticIntMethod(env, luajava_api_class, luajava_api_static_method_luaRead,
                                          (jint) stateIdx, byteBuffer, (jobject) ud);
  exp = (*env)->ExceptionOccurred(env);
  HANDLES_JAVA_EXCEPTION(L, exp, env, {
    free(buffer);
    return NULL;
  });
  if (len == 0) {
    if (buffer) {
      free(buffer);
    }
    return NULL;
  }
  *size = len;
  return buffer;
}

int luajavaCopyLuaFunctionWriter(lua_State *L, const void *p, size_t sz, void *ud) {
  struct LuaCopyData *buffer = ud;
  DEBUGF("[COPY] [Func] [Writer] buffer size: %d, head: %d, tail: %d\n", buffer->size, buffer->head, buffer->tail);
  if (sz + buffer->tail > buffer->size) {
    size_t newSize = buffer->size;
    do {
      newSize <<= 1;
    } while(sz + buffer->tail > newSize);

    char *data = (char *) malloc(sizeof(char) * newSize);
    memcpy(data, buffer->data, buffer->tail);
    free(buffer->data);
    buffer->data = data;
    buffer->size = newSize;
  }

  memcpy(buffer->data + buffer->tail, p, sz);
  buffer->tail += sz;
  return 0;
}

const char* luajavaCopyLuaFunctionReader(lua_State *L, void *ud, size_t *size) {
  struct LuaCopyData *buffer = ud;
  DEBUGF("[COPY] [Func] [Reader] buffer size: %d, head: %d, tail: %d\n", buffer->size, buffer->head, buffer->tail);
  if (buffer->tail <= buffer->head) {
    return NULL;
  }
  *size = buffer->tail - buffer->head;
  char *data = buffer->data + buffer->head;
  buffer->head = buffer->tail;
  return data;
}

int luajavaCopyLuaFunction(lua_State *srcL, int index, lua_State *destL, HashMap map) {
  // prepare copy buffer
  struct LuaCopyData *buffer = (struct LuaCopyData *) malloc(sizeof(struct LuaCopyData));
  buffer->size = LUAJAVA_COPY_DATA_BUFFER_SIZE;
  buffer->head = 0;
  buffer->tail = 0;
  buffer->data = (char *) malloc(sizeof(char) * buffer->size);

  // copy function to the top.
  lua_pushvalue(srcL, index);
  // dump from src
  DEBUG("[COPY] [Func] Start dump function\n");
  int ret = LUA_DUMP(srcL, luajavaCopyLuaFunctionWriter, buffer, 1);
  DEBUGF("[COPY] [Func] Dump function result: %d\n", ret);
  if (ret != LUA_OK) {
    free(buffer->data);
    free(buffer);
    lua_pop(srcL, 1);
    return 0;
  }

  // load to dest
  DEBUG("[COPY] [Func] Start load function\n");
  ret = LUA_LOAD(destL, luajavaCopyLuaFunctionReader, buffer, "CopiedClosure", "bt");
  DEBUGF("[COPY] [Func] Load function result: %d\n", ret);
  free(buffer->data);
  free(buffer);
  lua_pop(srcL, 1);
  if (ret != LUA_OK) return 0;

  // put function ref to map
  lua_pushvalue(destL, -1);
  size_t srcPtr = (size_t) lua_topointer(srcL, index);
  size_t destRef = luaL_ref(destL, LUA_REGISTRYINDEX);
  hashMap_put(map, srcPtr, destRef);

  // copy upvalues
  // pre-check the first upvalue is _ENV or not.
  int n = 1;
  const char *name = NULL;
  DEBUG("[COPY] [Func] Start pre-check the first upvalue\n");
  if ((name = lua_getupvalue(srcL, index, n)) != NULL) {
    DEBUGF("[COPY] [Func] The first upvalue is '%s'\n", name);
    if (strcmp(name, "_ENV") == 0) {
      // LUA 52, _ENV is nil
      {
        lua_getupvalue(destL, -1, n);
        if (lua_isnil(destL, -1)) {
          lua_pop(destL, 1);

          luaL_loadstring(destL, "return _ENV");
          lua_getupvalue(destL, -1, 1);
          lua_setupvalue(destL, -3, 1);
          lua_pop(destL, 1);
          lua_getupvalue(destL, -1, n);
        }
        lua_pop(destL, 1);
      }

      // COPY
      // lua_getupvalue(destL, -1, n);
      // luajavaDoCopyTableIfNotExists(srcL, -1, destL, map);
      // lua_setupvalue(destL, -2, n);

      DEBUGF("[COPY] [Func] The first upvalue is '%s', skip copy the first upvalue\n", name);
      n += 1;
    }
    lua_pop(srcL, 1);
  }

  DEBUGF("[COPY] [Func] Start copy upvalues, n = %d\n", n);
  while ((name = lua_getupvalue(srcL, index, n)) != NULL) {
    DEBUGF("[COPY] [Func] [%d] Copy upvalue '%s' type: %s\n", n, name, lua_typename(srcL, lua_type(srcL, -1)));
    if (!luajavaCopyLuaValue(srcL, -1, destL, map)) {
      DEBUGF("[COPY] [Func] [%d] failed copy upvalue '%s' type: %s\n", n, name, lua_typename(srcL, lua_type(srcL, -1)));
      lua_pop(srcL, 1);
      lua_pop(destL, 1);
      return 0;
    }
    name = lua_setupvalue(destL, -2, n);
    lua_pop(srcL, 1);
    n += 1;
    DEBUGF("[COPY] [Func] Upvalue copied\n", n);
  }
  DEBUGF("[COPY] [Func] Finished copy upvalues, n = %d\n", n);

  return 1;
}

int luajavaCopyLuaTable(lua_State *srcL, int index, lua_State *destL, HashMap map) {
  size_t srcPtr = (size_t) lua_topointer(srcL, index);

  // after pushed nil, index may need change.
  int offsetIndex = index < 0 ? index - 1 : index;
  lua_pushnil(srcL);
  lua_newtable(destL);

  // record table ref
  DEBUG("[COPY] [Table] Record table ref\n");
  lua_pushvalue(destL, -1);
  size_t destRef = luaL_ref(destL, LUA_REGISTRYINDEX);
  hashMap_put(map, srcPtr, destRef);

  DEBUG("[COPY] [Table] Start foreach source lua state.\n");
  while (lua_next(srcL, offsetIndex) != 0) {
    // copy key to dest lua state top
    if (!luajavaCopyLuaValue(srcL, -2, destL, map)) {
      DEBUGF("[COPY] [Table] Copy *key* '%s' failed\n", lua_typename(srcL, lua_type(srcL, -2)));
      lua_pop(srcL, 2);
      return 0;
    }

    // copy value to dest lua state top
    if (!luajavaCopyLuaValue(srcL, -1, destL, map)) {
      DEBUGF("[COPY] [Table] Copy *value* '%s' failed\n", lua_typename(srcL, lua_type(srcL, -1)));
      lua_pop(srcL, 2);
      lua_pop(destL, 1);
      return 0;
    }

    DEBUGF("[COPY] [TABLE] Finished copied a key-value pair: %s - %s\n",
          lua_typename(destL, lua_type(destL, -1)),
          lua_typename(destL, lua_type(destL, -2)));

    lua_pop(srcL, 1);
    lua_rawset(destL, -3);
  }
  return 1;
}

int luajavaCopyLuaValue(lua_State *srcL, int index, lua_State *destL, HashMap map) {
  // copy value
  switch(lua_type(srcL, index)) {
    case LUA_TNIL:
      DEBUG("[COPY] Start copy nil\n");
      lua_pushnil(destL);
      return 1;
    case LUA_TBOOLEAN:
      DEBUG("[COPY] Start copy boolean\n");
      lua_pushboolean(destL, lua_toboolean(srcL, index));
      return 1;
    case LUA_TLIGHTUSERDATA:
      DEBUG("[COPY] Start copy light userdata\n");
      lua_pushlightuserdata(destL, lua_touserdata(srcL, index));
      return 1;
    case LUA_TNUMBER:
      DEBUG("[COPY] Start copy number\n");
      lua_pushnumber(destL, lua_tonumber(srcL, index));
      return 1;
    case LUA_TSTRING:
      DEBUG("[COPY] Start copy string\n");
      lua_pushstring(destL, lua_tostring(srcL, index));
      return 1;
    case LUA_TUSERDATA: {
      DEBUG("[COPY] Start copy userdata\n");
      // copy java object
      int objectType = luajavaGetJavaObjectType(srcL, index);
      switch (objectType) {
        case LUAJAVA_OBJECT_TYPE_CLASS:
          pushJavaClass(destL, *((jobject *) lua_touserdata(srcL, index)));
          return 1;
        case LUAJAVA_OBJECT_TYPE_OBJECT:
          pushJavaObject(destL, *((jobject *) lua_touserdata(srcL, index)));
          return 1;
        case LUAJAVA_OBJECT_TYPE_ARRAY:
          pushJavaArray(destL, *((jobject *) lua_touserdata(srcL, index)));
          return 1;
      }
      break;
    }
    case LUA_TFUNCTION: {
      DEBUG("[COPY] Start copy function\n");
      size_t ptr = (size_t) lua_topointer(srcL, index);
      if (hashMap_get(map, ptr, &ptr)) {
        DEBUG("[COPY] Copy same function\n");
        lua_rawgeti(destL, LUA_REGISTRYINDEX, ptr);
        return 1;
      }
      return luajavaCopyLuaFunction(srcL, index, destL, map);
    }
    case LUA_TTABLE: {
      DEBUG("[COPY] Start copy table\n");
      size_t ptr = (size_t) lua_topointer(srcL, index);
      if (hashMap_get(map, ptr, &ptr)) {
        DEBUG("[COPY] Copy same table\n");
        lua_rawgeti(destL, LUA_REGISTRYINDEX, ptr);
        return 1;
      }
      return luajavaCopyLuaTable(srcL, index, destL, map);
    }
  }
  return 0;
}

int luajavaCopyLuaValueWrapper(lua_State *srcL, int idx, lua_State *destL) {
  DEBUGF("[COPY] [BEGIN] Start copy index `%d` to another lua state\n", idx);
  HashMap map = hashMap_new(32);
  int ret = luajavaCopyLuaValue(srcL, idx, destL, map);
  hashMap_foreach(map, key, value, {
    DEBUGF("[COPY] [END] [DEST] Unref '%d'\n", value);
    luaL_unref(destL, LUA_REGISTRYINDEX, value);
  });
  hashMap_free(map);
  return ret;
}

int luajavaDoCopyTableIfNotExists(lua_State *srcL, int index, lua_State *destL, HashMap map) {
  // if index is negtive, after pushed nil need change index
  int offsetIdx = index < 0 ? index - 1 : index;

  lua_pushnil(srcL);
  while (lua_next(srcL, offsetIdx)) {
    DEBUGF("[COPY_TABLE_IF_NOT_EXISTS] Copy key '%s'\n", 
            lua_isstring(srcL, -2) ? lua_tostring(srcL, -2) : "Not String");
    // copy key
    if (!luajavaCopyLuaValue(srcL, -2, destL, map)) {
      lua_pop(srcL, 1);
      continue;
    }

    // check nil
    lua_pushvalue(destL, -1);
    lua_rawget(destL, -3);
    if (!lua_isnil(destL, -1)) {
      DEBUGF("[COPY_TABLE_IF_NOT_EXISTS] Not nil, skip copy '%s'\n", 
              lua_isstring(srcL, -2) ? lua_tostring(srcL, -2) : "Not String");
      lua_pop(destL, 2);
      lua_pop(srcL, 1);
      continue;
    }
    lua_pop(destL, 1);

    // copy value
    if (!luajavaCopyLuaValue(srcL, -1, destL, map)) {
      lua_pop(destL, 1);
      lua_pop(srcL, 1);
      continue;
    }
    lua_rawset(destL, -3);
    lua_pop(srcL, 1);
  }
  return 1;
}

int luajavaCopyTableIfNotExists(lua_State *srcL, int index, lua_State *destL) {
  DEBUGF("[COPY_TABLE_IF_NOT_EXISTS] [BEGIN] Start copy index `%d` to another lua state\n", index);
  HashMap map = hashMap_new(32);
  int ret = luajavaDoCopyTableIfNotExists(srcL, index, destL, map);
  hashMap_foreach(map, key, value, {
    DEBUGF("[COPY_TABLE_IF_NOT_EXISTS] [END] [DEST] Unref '%d'\n", value);
    luaL_unref(destL, LUA_REGISTRYINDEX, value);
  });
  hashMap_free(map);
  return ret;
}

int luajavaNewGlobalEnv(lua_State *L) {
  lua_Integer gRef;
  lua_getglobal(L, "_G");
  lua_pushstring(L, "_LUAJAVA_G_REF");
  lua_gettable(L, -2);
  if (lua_isnumber(L, -1)) {
    gRef = lua_tointeger(L, -1);
    lua_pop(L, 2);
    lua_rawgeti(L, LUA_REGISTRYINDEX, gRef);
  } else {
    lua_pop(L, 1);
    // _G ref
    lua_pushvalue(L, -1);
    gRef = luaL_ref(L, LUA_REGISTRYINDEX);
    // push ref
    lua_pushstring(L, "_LUAJAVA_G_REF");
    lua_pushinteger(L, gRef);
    lua_rawset(L, -3);
  }

  // new _G table
  lua_newtable(L);
  // metatable
  lua_newtable(L);
  lua_pushstring(L, "__index");
  lua_pushvalue(L, -4);
  lua_rawset(L, -3);

  lua_setmetatable(L, -2);
  lua_setglobal(L, "_G");

#ifdef LUA_RIDX_GLOBALS
  lua_getglobal(L, "_G");
  lua_rawseti(L, LUA_REGISTRYINDEX, LUA_RIDX_GLOBALS);
#endif

  return 1;
}