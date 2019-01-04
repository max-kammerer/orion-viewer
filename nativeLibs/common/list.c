typedef struct Arraylist_Struct * Arraylist;

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/*
  constants
*/
#define ARRAYLIST_INITIAL_CAPACITY 256
#define ARRAYLIST_CAPACITY_DELTA 256

#define Object char

static const size_t object_size = sizeof(Object);
/*
  structures
*/
struct Arraylist_Struct {
  int _current_capacity;
  Object *_data;
  int _size;
};


void arraylist_free(const Arraylist list)
{
  free(list->_data);
  free(list);
}

Arraylist arraylist_create()
{
  Arraylist list;
  list = malloc(sizeof(struct Arraylist_Struct));
  list->_current_capacity = ARRAYLIST_INITIAL_CAPACITY;
  list->_data = malloc(object_size * list->_current_capacity);
  list->_size = 0;
  return list;
}

int arraylist_size(const Arraylist list)
{
  return list->_size;
}

int arraylist_is_empty(const Arraylist list)
{
  return (0 == arraylist_size(list));
}

int arraylist_add(const Arraylist list, Object object)
{
  int old_size = arraylist_size(list);
  int new_capacity;
  Object *new_data;

  (list->_size)++;
  if (old_size == list->_current_capacity)
    {
      new_capacity = list->_current_capacity + ARRAYLIST_CAPACITY_DELTA;
      new_data = malloc(object_size * new_capacity);
      memcpy(new_data, list->_data, object_size * old_size);
      free(list->_data);
      (list->_data) = new_data;
      list->_current_capacity = new_capacity;
    }
  (list->_data)[old_size] = object;
  return 1;
}

Object arraylist_get(const Arraylist list, const int index)
{
  return list->_data[index];
}

Object * arraylist_getData(const Arraylist list)
{
  return list->_data;
}

void arraylist_clear(const Arraylist list)
{
  list->_data = realloc(list->_data, object_size * ARRAYLIST_INITIAL_CAPACITY);
  list->_current_capacity = ARRAYLIST_INITIAL_CAPACITY;
  list->_size = 0;
}
