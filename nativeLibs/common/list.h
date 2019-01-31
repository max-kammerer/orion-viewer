typedef struct Arraylist_Struct * Arraylist;

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define Object char
/*
  structures
*/
struct Arraylist_Struct {
  int _current_capacity;
  Object *_data;
  int _size;
};


void arraylist_free(const Arraylist list);

Arraylist arraylist_create();

int arraylist_size(const Arraylist list);

int arraylist_is_empty(const Arraylist list);

int arraylist_add(const Arraylist list, Object object);

Object arraylist_get(const Arraylist list, const int index);

Object * arraylist_getData(const Arraylist list);

void arraylist_clear(const Arraylist list);
