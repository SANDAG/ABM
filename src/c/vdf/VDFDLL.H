#ifndef VDFDLL_
#define VDFDLL_

typedef struct vdf_flags_
{
    struct 
        {
        unsigned LinkTypesInUse:1;    // Apply link types
        unsigned TurnFlagInUse:1;    // Turn Penalty Flag
        unsigned :30;
        } flags;

    long unused[64];
}VDF_FLAGS;

#define VDF_LABELSIZE 64
  

#ifndef TC_OKAY
#define TC_OKAY      0     // normal return, okay
#define TC_NOTFOUND  2     // record not found
#define TC_INVINPUT  -934  // invalid input

#define short_miss  (-32767)
#define long_miss   (-2147483647)
#define flt_miss    ((float)(-3.402823466e+38))
#define dbl_miss   -1.7976931348623158e+308

#define short_min   (-32767)
#define short_max   (32767)
#define long_min    (-2147483647)
#define long_max    (2147483647)
#define flt_min     (1.175494351e-38F)
// for most negative float, use -flt_max.
#define flt_max     (3.402823466e+38F)
#define dbl_min     (-1.7976931348623158e+308)
#define dbl_max     (1.7976931348623158e+308)
#endif

// max is an undefined symbol if compiled on MVC
#ifndef max
#define max(a,b)    ((a) > (b) ? (a) : (b))
#endif
#ifndef min
#define min(a,b)    ((a) < (b) ? (a) : (b))
#endif

//---------------------------------------------------------------------
// Casting with rounding, setting missing values appropriately
//---------------------------------------------------------------------

static short  __s = 0;
static long   __l = 0;
static float  __f = 0;
static double __d = 0;

#define D2F(f) ((__d = (f)), (__d == dbl_miss  || __d   > flt_max   || __d   <  -flt_max ? flt_miss   : (float)  __d))
#define D2L(f) ((__d = (f)), (__d == dbl_miss  || __d+.5> long_max  || __d-.5<  long_min ? long_miss  : (long)   (__d+(__d < 0 ? -.5 : 0.5))))
#define D2S(f) ((__d = (f)), (__d == dbl_miss  || __d+.5> short_max || __d-.5< short_min ? short_miss : (short)  (__d+(__d < 0 ? -.5 : 0.5))))

#define F2D(f) ((__f = (f)), (__f == flt_miss                                            ? dbl_miss   : (double) __f))
#define F2L(f) ((__f = (f)), (__f == flt_miss  || __f+.5> long_max  || __f-.5<  long_min ? long_miss  : (long)   (__f+(__f < 0 ? -.5 : 0.5))))
#define F2S(f) ((__f = (f)), (__f == flt_miss  || __f+.5> short_max || __f-.5< short_min ? short_miss : (short)  (__f+(__f < 0 ? -.5 : 0.5))))

#define L2D(n) ((__l = (n)), (__l == long_miss                                           ? dbl_miss   : (double) __l))
#define L2F(n) ((__l = (n)), (__l == long_miss                                           ? flt_miss   : (float)  __l))
#define L2S(n) ((__l = (n)), (__l == long_miss || __l   > short_max || __l   < short_min ? short_miss : (short)  __l))

#define S2D(n) ((__s = (n)), (__s == short_miss                                          ? dbl_miss   : (double) __s))
#define S2F(n) ((__s = (n)), (__s == short_miss                                          ? flt_miss   : (float)  __s))
#define S2L(n) ((__s = (n)), (__s == short_miss                                          ? long_miss  : (long)   __s))

// _export is an obsolete keyword in MSC. Replaced with DLLEXPORT
// which is compiler dependent

#ifndef DLLEXPORT
#ifdef _MSC_VER
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT _export
#endif
#endif

#endif
