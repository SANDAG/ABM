//
// Volume Delay Function
//
//  Caliper Corporation 2007
//
//
#include <math.h>
#include <malloc.h>
#include <string.h>
#include <stdio.h>
#include <windows.h>
#include "vdfdll.h"

typedef enum field_
{
    T0,
    CAPACITY,
    CYCLE,
    PF,
    GC,
    ALPHA1, //for street segment delay
    BETA1,  //for street segment delay
    ALPHA2, //for intersection delay
    BETA2,  //for intersection delay
    PRELOAD,
    TYPE,
    CURRENT,
    N_FIELDS
} FIELD;

#define MINBETA 1.0001F

#ifndef NULL
#define NULL 0
#endif

#ifndef sqr
#define sqr(x) ((x) * (x))
#endif

static int          local_status = TC_OKAY;
static int          *_platform_tc_status = &local_status;
static double       BigReal = flt_max;
static float        Threshold = 0.;
static VDF_FLAGS    flags;

// Set parameter values and valid bounds
static char *fieldname[N_FIELDS - 2] =  {"Time", "Segment Capacity","Cycle","PF (Progression Factor)","GC (g/c ratio)",  "Alpha1 for Segment",  "Beta1 for Segment", "Alpha2 for Junction",  "Beta2 for Junction","Preload" };
static short Required[N_FIELDS-2] =     {1,1,1,1,1,1,1,1,1,0};
static short CheckBounds[N_FIELDS-2] =  {1,1,1,1,1,1,1,1,1,0};
static float LowerBound[N_FIELDS - 2] = {0,1,0,0.2,0,0,0,0,0,0};
static float UpperBound[N_FIELDS - 2] = {flt_max,flt_max,200,2,1,300, 300,300,300,flt_max};

// Include common VDF functions and preprocessir Utility
//#include "VDF_Include.c"

//
// VDF Include file with common functions
//
// Caliper Corporation 2007
//
//

void  DLLEXPORT  InitVDFDLL(int *ptc_status)
{
    _platform_tc_status = ptc_status;
}

void  DLLEXPORT  VDF_SetFlags(VDF_FLAGS *flags_)
{
    flags = *flags_;
}

long  DLLEXPORT  VDF_GetNParameters( void )
{
    return N_FIELDS - 2;
}

short  DLLEXPORT  VDF_GetParameters(char **param_names)
{
    short i;

    for (i=0; i < N_FIELDS - 2; i++)
        {
        strncpy(&(param_names[i][0]), fieldname[i], VDF_LABELSIZE);
        }

    return *_platform_tc_status = TC_OKAY;
}

short  DLLEXPORT  VDF_Preprocessor(float **Cost, long *links, void *defaults, short *UnUsed1, short *Unused2,  float *MaxVector, float *MaxCurrCost)
{
    long    i, j, k = 0;
    short   status = TC_OKAY;
    float   *v, *pDef;
    int     style = MB_TOPMOST|MB_APPLMODAL;   // MB_TASKMODAL
    char    ErrorMsg[200];

    *MaxCurrCost = 0.0;
    Required[0] = 1;        // T0 is always required

    for (i = 0; i < N_FIELDS -2; i++)
        MaxVector[i] = -flt_max;

    for (i = 0; i < *links; i++)
        {
        pDef = (float*) defaults;
        for (j = 0; j < N_FIELDS - 2; j++, pDef++)
            {
            v = &Cost[j][i];
            switch (j)
                {
                case PRELOAD:
                    if (*v == flt_miss || *v < 0.0)
                        *v = 0.0;
                    break;
                default:
                    if ( *v == flt_miss )               // if value missing
                        *v = *pDef;                         // use default
                }  // end of switch


            // Now check if the value is valid
            if ( Required[j] && *v == flt_miss )
                {
                // Return a 1 - based index for consistency with the network macro functions. In Net.dll look at 0 - based value
                sprintf(ErrorMsg, "No value for required field '%s' at link index %lu", fieldname[j], i + 1);
                status = TC_INVINPUT;
                goto Exit;
                }

            if ( CheckBounds[j] && (*v < LowerBound[j] || *v > UpperBound[j]) )
                {
                // Return a 1 - based index for consistency with the network macro functions. In Net.dll look at 0 - based value
                sprintf(ErrorMsg, "Value out of bounds for field '%s' at link index %lu", fieldname[j],i + 1);
                status = TC_INVINPUT;
                goto Exit;
                }
            }

        Cost[CURRENT][i] = Cost[T0][i];
        *MaxCurrCost = max(*MaxCurrCost, Cost[CURRENT][i]);
        for (j=0; j < N_FIELDS - 2; j++)
            MaxVector[j] = max(MaxVector[j], Cost[j][i]);
        }
    status = TC_OKAY;

Exit:
    if (status != TC_OKAY)
        {
        MessageBox(NULL, ErrorMsg, "Error", style|MB_OK|MB_ICONSTOP);
        }
    return( *_platform_tc_status = status);

}

short  DLLEXPORT  VDF_GetDefaults(double *d)
{
    d[T0]        =   flt_miss;
    d[CAPACITY]  =   flt_miss;
    d[CYCLE]     =   90;
    d[PF]        =   1.0;
    d[GC]        =   0.41;
    d[ALPHA1]    =   1.9;
    d[BETA1]     =   1.9;
    d[ALPHA2]    =   2.0;
    d[BETA2]     =   2.4;
    d[PRELOAD]   =   0.;

    return *_platform_tc_status = TC_OKAY;
}

short  DLLEXPORT  VDF_GetLabel(char *label)
{
    strncpy(label,  "Tucson's link-junction VDF",   VDF_LABELSIZE);
    return *_platform_tc_status = TC_OKAY;
}

static double TucsonVDF(double Flow, long link, float **Cost) //this is the whole BPR function
{
    double ratio_s , ratio_j, ratio4_s, ratio4_j, lambda_s, lambda_j, val;

    if (Cost[T0][link] == flt_miss || Cost[CAPACITY][link] == flt_miss ) // Uncapacitated link, or no previous travel time or no g/c ratio
        val = (double)Cost[T0][link];
    else
    {
        if (Cost[CAPACITY][link] > 0.0) //valid capacity
        {
            ratio_s = (Flow + Cost[PRELOAD][link])/ Cost[CAPACITY][link];//v/c ratio
            if(Cost[GC][link] > 0.0) // valid intersection signal g/c ratio
            	ratio_j = (Flow + Cost[PRELOAD][link]) * 0.85/ (Cost[CAPACITY][link]*Cost[GC][link]);//v/c ratio for junction assuming the percentage of through traffic is 85%.
            else
            	ratio_j = BigReal;
		}
        else
            ratio_s = ratio_j = BigReal;

        if (ratio_s >= BigReal)
            ratio4_s = ratio_s;
        else
            ratio4_s = pow(ratio_s, Cost[BETA1][link]);
        lambda_s = 1.0 + Cost[ALPHA1][link] * ratio4_s;
        val =  (double)Cost[T0][link] * lambda_s; //segment travel time

        if (ratio_j < BigReal)
        {

            ratio4_j = pow(ratio_j, Cost[BETA2][link]);
	        lambda_j = 1.0 + Cost[ALPHA2][link] * ratio4_j;
	        val += (Cost[PF][link]*Cost[CYCLE][link]/2. * pow((1-Cost[GC][link]),2.0) * lambda_j); //intersection delay time
		}
    }

    return(val); //return the travel time which is the cost after BPR adjustment for single link
}

double  DLLEXPORT  VDFValue(double *FlowVal, long *link, float **Cost, short *DisabledLinks) //get the VDF value for each individual network link
{
    double val;

    *_platform_tc_status = TC_OKAY;

    if (DisabledLinks[*link])
        return(flt_max);

    val = TucsonVDF(*FlowVal, *link, Cost);

    return(val);
}

void  DLLEXPORT  VDFValues(double *Flow, float **Cost, long *links, short *DisabledLinks) //get the VDF value for a group of network links
{
    long  link;

    *_platform_tc_status = TC_OKAY;
    Threshold  = 0.;

    for (link = 0; link < *links ; link++ )
    {
        if (DisabledLinks[link])
            continue;

        if (Cost[T0][link] == flt_miss) //no link travel time
        {
            Cost[CURRENT][link] = flt_miss; //travel time after the VPR adjustment is same as before, invalid
            continue;
        }

        Cost[CURRENT][link] = (float)TucsonVDF(Flow[link], link, Cost); //travel time after the VPR adjustment

        if (Cost[CURRENT][link] < Cost[T0][link])
        {
            *_platform_tc_status = TC_INVINPUT;
            Cost[CURRENT][link] = Cost[T0][link];//make the input is invalid, current travel time is same as the previous one
        }

        Threshold = max(Threshold, Cost[CURRENT][link]);
    }
}

// This is for VDF's that do not work on the time domain. TransCAD requires the knowledge of "pure" time for
// reporting purposes and for speed calculations
void  DLLEXPORT  VDFTimeOnly(double *Flow, float **Cost, long *links, short *DisabledLinks)
{
    VDFValues(Flow, Cost, links, DisabledLinks);
}

short  DLLEXPORT  VDF_Preprocess(float **Cost, long *links, void *defaults, short *Unused1, short *Unused2)
{
    short s;
    float MaxVector[N_FIELDS - 2], MaxCurrCost;

    s = VDF_Preprocessor(Cost, links, defaults, Unused1, Unused2, &MaxVector[0], &MaxCurrCost);


    Threshold = MaxCurrCost;
    BigReal = pow(flt_max, 1/MaxVector[BETA1]);

    return( *_platform_tc_status = s);
}



// Optional functions
double  DLLEXPORT  VDF_Derivative(double *FlowVal, long *link, float **Cost, short *DisabledLinks)
{
    double pow1_s, pow1_j,vc_s,vc_j, lambda_s, lambda_j, return_var;

    *_platform_tc_status = TC_OKAY;

    if (DisabledLinks[*link])
        return(0.);

    if (Cost[T0][*link] == flt_miss || Cost[CAPACITY][*link] <= 0 || Cost[CAPACITY][*link] == flt_miss ) //if no FFT, capacity or g/c ratio
        return(0.);
    else
    {
		//Segment part
      vc_s = (*FlowVal + Cost[PRELOAD][*link]) / Cost[CAPACITY][*link];
      pow1_s = pow(vc_s, Cost[BETA1][*link] - 1.0);
      lambda_s = Cost[ALPHA1][*link]  * Cost[BETA1][*link] * pow1_s * (1.0 / Cost[CAPACITY][*link]) ;
      return_var = (double)Cost[T0][*link] * lambda_s;

      //junction part

	  if(Cost[GC][*link] >0)
	  {
      	vc_j = (*FlowVal + Cost[PRELOAD][*link]) * 0.85/ (Cost[CAPACITY][*link]*Cost[GC][*link]);
      	pow1_j = pow(vc_j, Cost[BETA2][*link] - 1.0);
     	lambda_j = Cost[ALPHA2][*link]  * Cost[BETA2][*link]  * pow1_j * (0.85 / (Cost[CAPACITY][*link]*Cost[GC][*link]));

      	return_var += ((double)Cost[PF][*link]*Cost[CYCLE][*link]/2. * pow((1-Cost[GC][*link]),2.0) * lambda_j);
  	  }


      return(return_var);
    }
}

double  DLLEXPORT  VDF_Integral(double *FlowVal, long *link, float **Cost, short *DisabledLinks)
{
    double ratio_s,ratio_j, pow1_s, pow1_j, lambda_s,lambda_j, return_var;

    *_platform_tc_status = TC_OKAY;

    if (DisabledLinks[*link])
        return(0.);

    if (Cost[T0][*link] == flt_miss || Cost[CAPACITY][*link] <= 0 || Cost[CAPACITY][*link] == flt_miss || *FlowVal <= 0.)
        return 0.;
    else
    {

		//segment part
      ratio_s = (*FlowVal + Cost[PRELOAD][*link]) / Cost[CAPACITY][*link];
      pow1_s = pow(ratio_s, Cost[BETA1][*link]);
      lambda_s = 1.0 + pow1_s * Cost[ALPHA1][*link] / (Cost[BETA1][*link] + 1.0);
      return_var = (double)Cost[T0][*link] * (*FlowVal) * lambda_s;

		//junction part
	  if(Cost[GC][*link] > 0)
	  {
      	ratio_j = (*FlowVal + Cost[PRELOAD][*link]) * 0.85/ (Cost[CAPACITY][*link]*Cost[GC][*link]);
      	pow1_j = pow(ratio_j, Cost[BETA2][*link]);
      	lambda_j = 1.0 + pow1_j * Cost[ALPHA2][*link] / (Cost[BETA2][*link] + 1.0);
      	return_var += (double)Cost[PF][*link]*Cost[CYCLE][*link]/2. * pow((1-Cost[GC][*link]),2.0)*(*FlowVal) * lambda_j;
  	  }

      return(return_var);
    }
}

