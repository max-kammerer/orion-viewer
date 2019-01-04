#include <limits.h>	/* INT_MAX & co */

/* Rectangles and bounding boxes */
#define SAFE_INT(f) ((f > INT_MAX) ? INT_MAX : ((f < INT_MIN) ? INT_MIN : (int)f))

/*
	fz_is_empty_rect: Check if rectangle is empty.

	An empty rectangle is defined as one whose area is zero.
*/
#define fz_is_empty_rect(r) ((r).x0 == (r).x1 || (r).y0 == (r).y1)

/*
	fz_is_empty_bbox: Check if bounding box is empty.

	Same definition of empty bounding boxes as for empty
	rectangles. See fz_is_empty_rect.
*/
#define fz_is_empty_bbox(b) ((b).x0 == (b).x1 || (b).y0 == (b).y1)

/*
	fz_is_infinite: Check if rectangle is infinite.

	An infinite rectangle is defined as one where either of the
	two relationships between corner coordinates are not true.
*/
#define fz_is_infinite_rect(r) ((r).x0 > (r).x1 || (r).y0 > (r).y1)

/*
	fz_is_infinite_bbox: Check if bounding box is infinite.

	Same definition of infinite bounding boxes as for infinite
	rectangles. See fz_is_infinite_rect.
*/
#define fz_is_infinite_bbox(b) ((b).x0 > (b).x1 || (b).y0 > (b).y1)


static inline float fz_min(float a, float b)
{
	return (a < b ? a : b);
}

static inline int fz_mini(int a, int b)
{
	return (a < b ? a : b);
}

static inline float fz_max(float a, float b)
{
	return (a > b ? a : b);
}

static inline int fz_maxi(int a, int b)
{
	return (a > b ? a : b);
}


struct fz_rect_s
{
	float x0, y0;
	float x1, y1;
};

typedef struct fz_rect_s fz_rect;


struct fz_bbox_s
{
	int x0, y0;
	int x1, y1;
};

typedef struct fz_bbox_s fz_bbox;

const fz_rect fz_infinite_rect = { 1, 1, -1, -1 };
const fz_rect fz_empty_rect = { 0, 0, 0, 0 };
const fz_rect fz_unit_rect = { 0, 0, 1, 1 };

const fz_bbox fz_infinite_bbox = { 1, 1, -1, -1 };
const fz_bbox fz_empty_bbox = { 0, 0, 0, 0 };
const fz_bbox fz_unit_bbox = { 0, 0, 1, 1 };



fz_bbox
fz_bbox_covering_rect(fz_rect f)
{
	fz_bbox i;
	f.x0 = floorf(f.x0);
	f.y0 = floorf(f.y0);
	f.x1 = ceilf(f.x1);
	f.y1 = ceilf(f.y1);
	i.x0 = SAFE_INT(f.x0);
	i.y0 = SAFE_INT(f.y0);
	i.x1 = SAFE_INT(f.x1);
	i.y1 = SAFE_INT(f.y1);
	return i;
}

fz_bbox
fz_round_rect(fz_rect f)
{
	fz_bbox i;
	f.x0 = floorf(f.x0 + 0.001);
	f.y0 = floorf(f.y0 + 0.001);
	f.x1 = ceilf(f.x1 - 0.001);
	f.y1 = ceilf(f.y1 - 0.001);
	i.x0 = SAFE_INT(f.x0);
	i.y0 = SAFE_INT(f.y0);
	i.x1 = SAFE_INT(f.x1);
	i.y1 = SAFE_INT(f.y1);
	return i;
}

fz_rect
fz_intersect_rect(fz_rect a, fz_rect b)
{
	fz_rect r;
	if (fz_is_infinite_rect(a)) return b;
	if (fz_is_infinite_rect(b)) return a;
	if (fz_is_empty_rect(a)) return fz_empty_rect;
	if (fz_is_empty_rect(b)) return fz_empty_rect;
	r.x0 = fz_max(a.x0, b.x0);
	r.y0 = fz_max(a.y0, b.y0);
	r.x1 = fz_min(a.x1, b.x1);
	r.y1 = fz_min(a.y1, b.y1);
	return (r.x1 < r.x0 || r.y1 < r.y0) ? fz_empty_rect : r;
}

fz_rect
fz_union_rect(fz_rect a, fz_rect b)
{
	fz_rect r;
	if (fz_is_infinite_rect(a)) return a;
	if (fz_is_infinite_rect(b)) return b;
	if (fz_is_empty_rect(a)) return b;
	if (fz_is_empty_rect(b)) return a;
	r.x0 = fz_min(a.x0, b.x0);
	r.y0 = fz_min(a.y0, b.y0);
	r.x1 = fz_max(a.x1, b.x1);
	r.y1 = fz_max(a.y1, b.y1);
	return r;
}

fz_bbox
fz_intersect_bbox(fz_bbox a, fz_bbox b)
{
	fz_bbox r;
	if (fz_is_infinite_rect(a)) return b;
	if (fz_is_infinite_rect(b)) return a;
	if (fz_is_empty_rect(a)) return fz_empty_bbox;
	if (fz_is_empty_rect(b)) return fz_empty_bbox;
	r.x0 = fz_maxi(a.x0, b.x0);
	r.y0 = fz_maxi(a.y0, b.y0);
	r.x1 = fz_mini(a.x1, b.x1);
	r.y1 = fz_mini(a.y1, b.y1);
	return (r.x1 < r.x0 || r.y1 < r.y0) ? fz_empty_bbox : r;
}

fz_bbox
fz_union_bbox(fz_bbox a, fz_bbox b)
{
	fz_bbox r;
	if (fz_is_infinite_rect(a)) return a;
	if (fz_is_infinite_rect(b)) return b;
	if (fz_is_empty_rect(a)) return b;
	if (fz_is_empty_rect(b)) return a;
	r.x0 = fz_mini(a.x0, b.x0);
	r.y0 = fz_mini(a.y0, b.y0);
	r.x1 = fz_maxi(a.x1, b.x1);
	r.y1 = fz_maxi(a.y1, b.y1);
	return r;
}

