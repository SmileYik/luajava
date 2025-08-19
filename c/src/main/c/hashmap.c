#include <string.h>
#include <stdlib.h>
#include "hashmap.h"

size_t hashMap_hash(size_t key, size_t size) {
  return key % size;
}

HashMap hashMap_new(size_t size) {
  size_t nodesLen = sizeof(struct HashMapNode) * size;
  HashMap map = (HashMap) malloc(sizeof(struct _HashMap));
  map->size = size;
  map->hash = hashMap_hash;
  map->nodes = (struct HashMapNode *) malloc(nodesLen);
  memset(map->nodes, 0, nodesLen);
  return map;
}

void hashMap_put(HashMap map, size_t key, size_t value) {
  size_t hash = map->hash(key, map->size);
  struct HashMapNode *prev = map->nodes + hash;
  struct HashMapNode *next = prev->next;
  while (next && key != next->key) {
    prev = next;
    next = next->next;
  }
  if (next) {
    next->value = value;
  } else {
    prev->next = (struct HashMapNode *) malloc(sizeof(struct HashMapNode));
    prev->next->key = key;
    prev->next->value = value;
    prev->next->next = NULL;
  }
}

int hashMap_get(HashMap map, size_t key, size_t *value) {
  size_t hash = map->hash(key, map->size);
  struct HashMapNode *node = map->nodes + hash;
  node = node->next;
  if (!node) return 0;
  while (node && key != node->key) {
    node = node->next;
  }
  if (node) {
    *value = node->value;
    return 1;
  }
  return 0;
}

int hashMap_contains(HashMap map, size_t key) {
  size_t hash = map->hash(key, map->size);
  struct HashMapNode *node = map->nodes + hash;
  node = node->next;

  while (node && key != node->key) {
    node = node->next;
  }
  return node != NULL;
}

void hashMap_remove(HashMap map, size_t key) {
  size_t hash = map->hash(key, map->size);
  struct HashMapNode *prev = map->nodes + hash;
  struct HashMapNode *next = prev->next;
  if (!next) return;
  while (next && key != next->key) {
    prev = next;
    next = next->next;
  }
  if (next) {
    prev->next = next->next;
    free(next);
  }
}

void hashMap_free(HashMap map) {
  if (!map) return;
  struct HashMapNode *node = map->nodes;
  struct HashMapNode *next, *temp;
  size_t size = map->size;
  while (size) {
    next = node->next;
    while (next) {
      temp = next;
      next = next->next;
      free(temp);
    }
    size -= 1;
    node += 1;
  }
  free(map->nodes);
  free(map);
}