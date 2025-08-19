#ifndef HASHMAP_H
#define HASHMAP_H

typedef size_t (*HashMapHash) (size_t key, size_t mapSize);
typedef struct _HashMap* HashMap;

struct HashMapNode {
  size_t key;
  size_t value;
  struct HashMapNode *next;
};

struct _HashMap {
  struct HashMapNode* nodes;
  size_t size;
  HashMapHash hash;
};

#define hashMap_foreach(MAP, KEY, VALUE, CODE) \
  { \
    if (MAP) { \
      size_t KEY; \
      size_t VALUE; \
      \
      struct HashMapNode *node = MAP->nodes; \
      struct HashMapNode *next; \
      size_t size = MAP->size; \
      while (size) { \
        next = node->next; \
        while (next) { \
          KEY = next->key; \
          VALUE = next->value; \
          CODE \
          next = next->next; \
        } \
        size -= 1; \
        node += 1; \
      } \
    } \
  }


size_t hashMap_hash(size_t key, size_t size);

HashMap hashMap_new(size_t size);

void hashMap_put(HashMap map, size_t key, size_t value);

int hashMap_get(HashMap map, size_t key, size_t *value);

int hashMap_contains(HashMap map, size_t key);

void hashMap_remove(HashMap map, size_t key);

void hashMap_free(HashMap map);

#endif // HASHMAP_H