#include <assert.h>

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


// structure to store VDF related data
typedef struct _VDFDATA {
    char    *szVDFName;
    short   Fortran77;              // if a dll is a FORTRAN dll
    long    nParameters;
    long    nConstants;
    char    **ParameterNames;       // VDF Parameter names
    char    **LinkFieldNames;       // network field names used for each of the parameters
    long    *ParameterIndices;
    float   *DefaultValues;
    double  *Constants;
    short   *DisabledLinks;
    float   *FlatCost;
    float   **Cost;
} VDFDATA;

typedef struct _VDFDATA *VDF_DATA;

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

#ifndef HAVE_ROUNDING_FUNCTIONS
//---------------------------------------------------------------------
// Casting with rounding, setting missing values appropriately
//---------------------------------------------------------------------
static float  D2F(double d) { return d == dbl_miss  || d   > flt_max   || d   <  -flt_max ? flt_miss   : (float)  d; }
static long   D2L(double d) { return d == dbl_miss  || d+.5> long_max  || d-.5<  long_min ? long_miss  : (long)   (d+(d < 0 ? -.5 : 0.5)); }
static short  D2S(double d) { return d == dbl_miss  || d+.5> short_max || d-.5< short_min ? short_miss : (short)  (d+(d < 0 ? -.5 : 0.5)); }

static double F2D(float  f) { return f == flt_miss                                        ? dbl_miss   : (double) f; }
static long   F2L(float  f) { return f == flt_miss  || f+.5> long_max  || f-.5<  long_min ? long_miss  : (long)   (f+(f < 0 ? -.5 : 0.5)); }
static short  F2S(float  f) { return f == flt_miss  || f+.5> short_max || f-.5< short_min ? short_miss : (short)  (f+(f < 0 ? -.5 : 0.5)); }
              
static double L2D(long   l) { return l == long_miss                                       ? dbl_miss   : (double) l; }
static float  L2F(long   l) { return l == long_miss                                       ? flt_miss   : (float)  l; }
static short  L2S(long   l) { return l == long_miss || l   > short_max || l   < short_min ? short_miss : (short)  l; }

static double S2D(short  s) { return s == short_miss                                          ? dbl_miss   : (double) s; }
static float  S2F(short  s) { return s == short_miss                                          ? flt_miss   : (float)  s; }
static long   S2L(short  s) { return s == short_miss                                          ? long_miss  : (long)   s; }
#endif

// _export is an obsolete keyword in MSC. Replaced with DLLEXPORT
// which is compiler dependent

#ifndef DLLEXPORT
#ifdef _MSC_VER
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT _export
#endif
#endif

#if defined(_MSC_VER) && !defined(_WIN64)
#define VDF_CALLING_CONV  __fastcall
#else
#define VDF_CALLING_CONV
#endif



#endif
