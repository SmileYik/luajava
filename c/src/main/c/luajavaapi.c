#include "luajavaapi.h"
#include "lauxlib.h"
#include <stdio.h>
#include <stdlib.h>

/* Handles exception */
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
      jmethodID methodId; \
      methodId = (*javaEnv)->GetMethodID(javaEnv, throwable_class, "toString", \
                                         "()Ljava/lang/String;"); \
      jStr = (*javaEnv)->CallObjectMethod(javaEnv, exp, methodId); \
    } \
    \
    cStr = (*javaEnv)->GetStringUTFChars(javaEnv, jStr, NULL); \
    \
    char errorStack[1 << 10] = ""; \
    sprintf(errorStack, "%s", cStr); \
    generateLuaStateStack(L, errorStack); \
    lua_pushstring(L, errorStack); \
    \
    (*javaEnv)->ReleaseStringUTFChars(javaEnv, jStr, cStr); \
    lua_error(L); \
  }

static jclass    throwable_class         = NULL;
static jmethodID get_message_method      = NULL;
static jclass    java_function_class     = NULL;
static jmethodID java_function_method    = NULL;
static jclass    java_lang_class         = NULL;

static jclass    luajava_api_class       = NULL;
static jmethodID luajava_api_method_checkField  = NULL;
static jmethodID luajava_api_method_checkMethod = NULL;


/********************* Implementations ***************************/

/***************************************************************************
 *
 *  Function: setupLuaJavaApi
 *  ****/

void setupLuaJavaApi(JNIEnv *env) {
  jclass tempClass;
  if (luajava_api_class == NULL) {
    tempClass = (*env)->FindClass(env, "org/keplerproject/luajava/LuaJavaAPI");

    if (tempClass == NULL) {
      fprintf(stderr, "Could not find LuaJavaAPI class\n");
      exit(1);
    }

    if ((luajava_api_class = (*env)->NewGlobalRef(env, tempClass)) == NULL) {
      fprintf(stderr, "Could not bind to LuaJavaAPI class\n");
      exit(1);
    }
  }

  if (luajava_api_method_checkField == NULL) {
    luajava_api_method_checkField = (*env)->GetStaticMethodID(env, luajava_api_class, 
                            "checkField", "(ILjava/lang/Object;Ljava/lang/String;)I");
    if (!luajava_api_method_checkField) {
      fprintf(stderr, "Could not find <checkField> method in LuaJavaApi\n");
      exit(1);
    }
  }

  if (luajava_api_method_checkMethod == NULL) {
    luajava_api_method_checkMethod = (*env)->GetStaticMethodID(env, luajava_api_class, 
                            "checkMethod", "(ILjava/lang/Object;Ljava/lang/String;)Z");
    if (!luajava_api_method_checkMethod) {
      fprintf(stderr, "Could not find <checkMethod> method in LuaJavaApi\n");
      exit(1);
    }
  }

  if (java_function_class == NULL) {
    tempClass =
        (*env)->FindClass(env, "org/keplerproject/luajava/JavaFunction");

    if (tempClass == NULL) {
      fprintf(stderr, "Could not find JavaFunction interface\n");
      exit(1);
    }

    if ((java_function_class = (*env)->NewGlobalRef(env, tempClass)) == NULL) {
      fprintf(stderr, "Could not bind to JavaFunction interface\n");
      exit(1);
    }
  }

  if (java_function_method == NULL) {
    java_function_method =
        (*env)->GetMethodID(env, java_function_class, "execute", "()I");
    if (!java_function_method) {
      fprintf(stderr, "Could not find <execute> method in JavaFunction\n");
      exit(1);
    }
  }

  if (throwable_class == NULL) {
    tempClass = (*env)->FindClass(env, "java/lang/Throwable");

    if (tempClass == NULL) {
      fprintf(stderr, "Error. Couldn't bind java class java.lang.Throwable\n");
      exit(1);
    }

    throwable_class = (*env)->NewGlobalRef(env, tempClass);

    if (throwable_class == NULL) {
      fprintf(stderr, "Error. Couldn't bind java class java.lang.Throwable\n");
      exit(1);
    }
  }

  if (get_message_method == NULL) {
    get_message_method = (*env)->GetMethodID(env, throwable_class, "getMessage",
                                             "()Ljava/lang/String;");

    if (get_message_method == NULL) {
      fprintf(stderr,
              "Could not find <getMessage> method in java.lang.Throwable\n");
      exit(1);
    }
  }

  if (java_lang_class == NULL) {
    tempClass = (*env)->FindClass(env, "java/lang/Class");

    if (tempClass == NULL) {
      fprintf(stderr, "Error. Coundn't bind java class java.lang.Class\n");
      exit(1);
    }

    java_lang_class = (*env)->NewGlobalRef(env, tempClass);

    if (java_lang_class == NULL) {
      fprintf(stderr, "Error. Couldn't bind java class java.lang.Throwable\n");
      exit(1);
    }
  }
}

/***************************************************************************
 *
 *  Function: getLuaStateIndex
 *  ****/

static lua_Number getLuaStateIndex(lua_State *L) {
  lua_Number stateIndex;
  /* Gets the luaState index */
  lua_pushstring(L, LUAJAVASTATEINDEX);
  lua_rawget(L, LUA_REGISTRYINDEX);

  if (!lua_isnumber(L, -1)) {
    throwLuaError(L, "Impossible to identify luaState id.");
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
    sprintf(stack_str, "%s\n\t[Stack %d] [%s] %s: %s (%s#%d)", stack_str,
            level - 1, ar.what ? ar.what : "(unknown what)",
            ar.namewhat ? ar.namewhat : "(unknown namewhat)",
            ar.name ? ar.name : "(unknown name)",
            ar.source ? ar.source : "(unknown source)", ar.currentline);
  }
}

/***************************************************************************
 *
 *  Function: throwLuaError
 *  ****/

void throwLuaError(lua_State *L, const char *msg) {
  char errorStack[1 << 10] = "";
  sprintf(errorStack, "%s", msg);
  generateLuaStateStack(L, errorStack);
  lua_pushstring(L, errorStack);
  lua_error(L);
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

  if (!lua_isstring(L, -1)) {
    throwLuaError(L, "Invalid object index. Must be string.");
  }

  key = lua_tostring(L, -1);

  if (!isJavaObject(L, 1)) {
    throwLuaError(L, "Not a valid Java Object.");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  obj = (jobject *)lua_touserdata(L, 1);

  str = (*javaEnv)->NewStringUTF(javaEnv, key);

  checkField = (*javaEnv)->CallStaticIntMethod(
      javaEnv, luajava_api_class, luajava_api_method_checkField, (jint)stateIndex, *obj, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  if (checkField != 0) {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
    return checkField;
  }

  checkMethodRet = (*javaEnv)->CallStaticIntMethod(
      javaEnv, luajava_api_class, luajava_api_method_checkMethod, (jint)stateIndex, *obj, str);
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
    throwLuaError(L, "Invalid MetaTable.");
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
  jmethodID method;
  jthrowable exp;
  const char *methodName;
  jint ret;
  jstring str;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  /* Checks if is a valid java object */
  if (!isJavaObject(L, 1)) {
    throwLuaError(L, "Not a valid OO function call.");
  }

  lua_getmetatable(L, 1);
  if (lua_type(L, -1) == LUA_TNIL) {
    throwLuaError(L, "Not a valid java Object.");
  }

  /* Gets the method Name */
  lua_pushstring(L, LUAJAVAOBJFUNCCALLED);
  lua_rawget(L, -2);
  if (lua_type(L, -1) == LUA_TNIL) {
    throwLuaError(L, "Not a OO function call.");
  }
  methodName = lua_tostring(L, -1);

  lua_pop(L, 2);

  /* Gets the object reference */
  pObject = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  /* Gets method */
  method =
      (*javaEnv)->GetStaticMethodID(javaEnv, luajava_api_class, "objectIndex",
                                    "(ILjava/lang/Object;Ljava/lang/String;)I");

  str = (*javaEnv)->NewStringUTF(javaEnv, methodName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
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
  jmethodID method;
  const char *fieldName;
  jstring str;
  jint ret;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
    throwLuaError(L, "Not a valid java class.");
  }

  /* Gets the field Name */

  if (!lua_isstring(L, 2)) {
    throwLuaError(L, "Not a valid field call.");
  }

  fieldName = lua_tostring(L, 2);

  /* Gets the object reference */
  obj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  method = (*javaEnv)->GetStaticMethodID(
      javaEnv, luajava_api_class, "objectNewIndex",
      "(ILjava/lang/Object;Ljava/lang/String;)I");

  str = (*javaEnv)->NewStringUTF(javaEnv, fieldName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
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
  jmethodID method;
  const char *fieldName;
  jstring str;
  jint ret;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
    throwLuaError(L, "Not a valid java class.");
  }

  /* Gets the field Name */

  if (!lua_isstring(L, 2)) {
    throwLuaError(L, "Not a valid field call.");
  }

  fieldName = lua_tostring(L, 2);

  /* Gets the object reference */
  obj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  method =
      (*javaEnv)->GetStaticMethodID(javaEnv, luajava_api_class, "classIndex",
                                    "(ILjava/lang/Class;Ljava/lang/String;)I");

  str = (*javaEnv)->NewStringUTF(javaEnv, fieldName);

  /* Return 1 for field, 2 for method or 0 for error */
  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
                                        (jint)stateIndex, *obj, str);

  exp = (*javaEnv)->ExceptionOccurred(javaEnv);
  HANDLES_JAVA_EXCEPTION(L, exp, javaEnv, {
    (*javaEnv)->DeleteLocalRef(javaEnv, str);
  });

  (*javaEnv)->DeleteLocalRef(javaEnv, str);

  if (ret == 0) {
    throwLuaError(L, "Name is not a static field or function.");
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
  jmethodID method;
  jint ret;
  jobject *obj;
  jthrowable exp;
  JNIEnv *javaEnv;

  /* Can index as number or string */
  if (!lua_isnumber(L, -1) && !lua_isstring(L, -1)) {
    throwLuaError(L, "Invalid object index. Must be integer or string.");
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
    throwLuaError(L, "Not a valid Java Object.");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  obj = (jobject *)lua_touserdata(L, 1);

  method = (*javaEnv)->GetStaticMethodID(
      javaEnv, luajava_api_class, "arrayIndex", "(ILjava/lang/Object;I)I");

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
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
  jmethodID method;
  lua_Integer key;
  jint ret;
  jthrowable exp;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  if (!isJavaObject(L, 1)) {
    throwLuaError(L, "Not a valid java class.");
  }

  /* Gets the field Name */

  if (!lua_isnumber(L, 2)) {
    throwLuaError(L, "Not a valid array index.");
  }

  key = lua_tointeger(L, 2);

  /* Gets the object reference */
  obj = (jobject *)lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  method = (*javaEnv)->GetStaticMethodID(
      javaEnv, luajava_api_class, "arrayNewIndex", "(ILjava/lang/Object;I)I");

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
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
    throwLuaError(L, "Not a valid Java Object!");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
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
    throwLuaError(L, "Not a valid Java Object!");
  }

  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
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
    throwLuaError(L, "This object is not a instance of Map or Collection.");
  }

  method = (*javaEnv)->GetMethodID(javaEnv, clazz, "size", "()I");
  if (method == NULL) {
    throwLuaError(
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
    throwLuaError(L, "Invalid JNI Environment.");
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
  jmethodID method;
  JNIEnv *javaEnv;
  lua_Number stateIndex;
  jthrowable exp;
  jint ret;

  if (!isJavaObject(L, 1) && !isJavaObject(L, 2)) {
    throwLuaError(
        L, "In the concat operation, at least one java object is required.");
  }

  stateIndex = getLuaStateIndex(L);

  // java env
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  method = (*javaEnv)->GetStaticMethodID(javaEnv, luajava_api_class,
                                         "objectConcat", "(I)I");

  if (method == NULL) {
    throwLuaError(
        L, "Invalid method org.keplerproject.luajava.LuaJavaAPI.objectConcat.");
  }

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
                                        (jint)stateIndex);
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
    throwLuaError(L, "Invalid JNI Environment.");
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
  jmethodID method;
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
    throwLuaError(L, "Invalid JNI Environment.");
  }

  /* get the string parameter */
  if (!lua_isstring(L, 1)) {
    throwLuaError(L, "Invalid parameter type. String expected.");
  }
  className = lua_tostring(L, 1);

  method =
      (*javaEnv)->GetStaticMethodID(javaEnv, java_lang_class, "forName",
                                    "(Ljava/lang/String;)Ljava/lang/Class;");

  javaClassName = (*javaEnv)->NewStringUTF(javaEnv, className);

  classInstance = (*javaEnv)->CallStaticObjectMethod(javaEnv, java_lang_class,
                                                     method, javaClassName);

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
    throwLuaError(L, "Error. Function createProxy expects 2 arguments.");
  }

  stateIndex = getLuaStateIndex(L);

  if (!lua_isstring(L, 1) || !lua_istable(L, 2)) {
    throwLuaError(L, "Invalid Argument types. Expected (string, table).");
  }

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
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
  jclass clazz;
  jmethodID method;
  jobject classInstance;
  jthrowable exp;
  jobject *userData;
  lua_Number stateIndex;
  JNIEnv *javaEnv;

  top = lua_gettop(L);

  if (top == 0) {
    throwLuaError(L, "Error. Invalid number of parameters.");
  }

  stateIndex = getLuaStateIndex(L);

  /* Gets the java Class reference */
  if (!isJavaObject(L, 1)) {
    throwLuaError(L, "Argument not a valid Java Class.");
  }

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  clazz = (*javaEnv)->FindClass(javaEnv, "java/lang/Class");

  userData = (jobject *)lua_touserdata(L, 1);

  classInstance = (jobject)*userData;

  if ((*javaEnv)->IsInstanceOf(javaEnv, classInstance, clazz) == JNI_FALSE) {
    throwLuaError(L, "Argument not a valid Java Class.");
  }

  method = (*javaEnv)->GetStaticMethodID(javaEnv, luajava_api_class, "javaNew",
                                         "(ILjava/lang/Class;)I");

  if (clazz == NULL || method == NULL) {
    throwLuaError(
        L, "Invalid method org.keplerproject.luajava.LuaJavaAPI.javaNew.");
  }

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, clazz, method,
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
  jmethodID method;
  const char *className;
  jstring javaClassName;
  jthrowable exp;
  lua_Number stateIndex;
  JNIEnv *javaEnv;

  stateIndex = getLuaStateIndex(L);

  /* get the string parameter */
  if (!lua_isstring(L, 1)) {
    throwLuaError(
        L, "Invalid parameter type. String expected as first parameter.");
  }

  className = lua_tostring(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  method = (*javaEnv)->GetStaticMethodID(
      javaEnv, luajava_api_class, "javaNewInstance", "(ILjava/lang/String;)I");

  javaClassName = (*javaEnv)->NewStringUTF(javaEnv, className);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
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
  jmethodID method;
  jthrowable exp;
  jstring javaClassName, javaMethodName;
  JNIEnv *javaEnv;

  top = lua_gettop(L);

  if (top != 2) {
    throwLuaError(L, "Error. Invalid number of parameters.");
  }

  stateIndex = getLuaStateIndex(L);

  if (!lua_isstring(L, 1) || !lua_isstring(L, 2)) {
    throwLuaError(L, "Invalid parameter. Strings expected.");
  }

  className = lua_tostring(L, 1);
  methodName = lua_tostring(L, 2);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
  }

  method =
      (*javaEnv)->GetStaticMethodID(javaEnv, luajava_api_class, "javaLoadLib",
                                    "(ILjava/lang/String;Ljava/lang/String;)I");

  javaClassName = (*javaEnv)->NewStringUTF(javaEnv, className);
  javaMethodName = (*javaEnv)->NewStringUTF(javaEnv, methodName);

  ret = (*javaEnv)->CallStaticIntMethod(javaEnv, luajava_api_class, method,
                                        (jint)stateIndex, javaClassName,
                                        javaMethodName);

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
    throwLuaError(L, "Invalid JNI Environment.");
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
    throwLuaError(L, "Cannot create proxy to java class.");
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
    throwLuaError(L, "Invalid JNI Environment.");
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
    throwLuaError(L, "Cannot create proxy to java object.");
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
    throwLuaError(L, "Invalid JNI Environment.");
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
    throwLuaError(L, "Cannot create proxy to java object.");
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

  jclass classPtr = (*env)->GetObjectClass(env, cptr);
  jfieldID CPtr_peer_ID = (*env)->GetFieldID(env, classPtr, "peer", "J");
  jbyte *peer = (jbyte *)(*env)->GetLongField(env, cptr, CPtr_peer_ID);

  L = (lua_State *)peer;

  pushJNIEnv(env, L);

  return L;
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
    throwLuaError(L, "Not a java Function.");
  }

  obj = lua_touserdata(L, 1);

  /* Gets the JNI Environment */
  javaEnv = getEnvFromState(L);
  if (javaEnv == NULL) {
    throwLuaError(L, "Invalid JNI Environment.");
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