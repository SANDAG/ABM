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
//#include "stdafx.h"

typedef enum field_
{
	T0,
	CAPACITY,
	INT_CAPACITY,
	CYCLE,
	PF,
	GC,
	ALPHA1, //for street segment delay
	BETA1,  //for street segment delay
	ALPHA2, //for intersection delay
	BETA2,  //for intersection delay
	LOS_C_FACTOR,
	LOS_D_FACTOR,
	LOS_E_FACTOR,
	LOS_F_LOW_FACTOR,
	LOS_F_HIGH_FACTOR,
	STAT_RELIABILITY,
	LENGTH,
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

// for reliability thresholds
static float LOS_C_THRESHOLD = 0.7;
static float LOS_D_THRESHOLD = 0.8;
static float LOS_E_THRESHOLD = 0.9;
static float LOS_FL_THRESHOLD = 1.0;
static float LOS_FH_THRESHOLD = 1.2;

// Set parameter values and valid bounds
static char *fieldname[N_FIELDS - 2] = { "Time", "Segment Capacity", "Intersection Capacity", "Cycle", "PF (Progression Factor)", "GC (g/c ratio)",
"Alpha1 for Segment", "Beta1 for Segment", "Alpha2 for Junction", "Beta2 for Junction", "Preload", "LOS C reliability factor", "LOS D reliability factor", "LOS E reliability factor",
"LOS F.low reliability factor", "LOS F.high reliability factor", "Static link reliability factor", "Length" };
static short Required[N_FIELDS - 2] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 };
static short CheckBounds[N_FIELDS - 2] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
static float LowerBound[N_FIELDS - 2] = { 0, 1, 0, 0, 0.2, 0, 0, 0, 0, 0, -99, -99, -99, -99, -99, -99, 0 };
static float UpperBound[N_FIELDS - 2] = { flt_max, flt_max, flt_max, 200, 2, 1, 300, 300, 300, 300, flt_max, flt_max, flt_max, flt_max, flt_max, flt_max, flt_max, flt_max };

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

long  DLLEXPORT  VDF_GetNParameters(void)
{
	return N_FIELDS - 2;
}

short  DLLEXPORT  VDF_GetParameters(char **param_names)
{
	short i;

	for (i = 0; i < N_FIELDS - 2; i++)
	{
		strncpy(&(param_names[i][0]), fieldname[i], VDF_LABELSIZE);
	}

	return *_platform_tc_status = TC_OKAY;
}

short  DLLEXPORT  VDF_Preprocessor(float **Cost, long *links, void *defaults, short *UnUsed1, short *Unused2, float *MaxVector, float *MaxCurrCost)
{
	long    i, j, k = 0;
	short   status = TC_OKAY;
	float   *v, *pDef;
	int     style = MB_TOPMOST | MB_APPLMODAL;   // MB_TASKMODAL
	char    ErrorMsg[200];

	*MaxCurrCost = 0.0;
	Required[0] = 1;        // T0 is always required

	for (i = 0; i < N_FIELDS - 2; i++)
		MaxVector[i] = -flt_max;

	for (i = 0; i < *links; i++)
	{
		pDef = (float*)defaults;
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
				if (*v == flt_miss)               // if value missing
					*v = *pDef;                         // use default
			}  // end of switch


			// Now check if the value is valid
			if (Required[j] && *v == flt_miss)
			{
				// Return a 1 - based index for consistency with the network macro functions. In Net.dll look at 0 - based value
				sprintf(ErrorMsg, "No value for required field '%s' at link index %lu", fieldname[j], i + 1);
				status = TC_INVINPUT;
				goto Exit;
			}

			if (CheckBounds[j] && (*v < LowerBound[j] || *v > UpperBound[j]))
			{
				// Return a 1 - based index for consistency with the network macro functions. In Net.dll look at 0 - based value
				sprintf(ErrorMsg, "Value out of bounds for field '%s' at link index %lu", fieldname[j], i + 1);
				status = TC_INVINPUT;
				goto Exit;
			}
		}

		Cost[CURRENT][i] = Cost[T0][i];
		*MaxCurrCost = max(*MaxCurrCost, Cost[CURRENT][i]);
		for (j = 0; j < N_FIELDS - 2; j++)
			MaxVector[j] = max(MaxVector[j], Cost[j][i]);
	}
	status = TC_OKAY;

Exit:
	if (status != TC_OKAY)
	{
		MessageBox(NULL, ErrorMsg, "Error", style | MB_OK | MB_ICONSTOP);
	}
	return(*_platform_tc_status = status);

}

short  DLLEXPORT  VDF_GetDefaults(double *d)
{
	d[T0] = flt_miss;
	d[CAPACITY] = flt_miss;
	d[INT_CAPACITY] = flt_miss;
	d[CYCLE] = 90;
	d[PF] = 1.0;
	d[GC] = 0.41;
	d[ALPHA1] = 1.9;
	d[BETA1] = 1.9;
	d[ALPHA2] = 2.0;
	d[BETA2] = 2.4;
	d[PRELOAD] = 0.;
	d[LOS_C_FACTOR] = 0.;
	d[LOS_D_FACTOR] = 0.;
	d[LOS_E_FACTOR] = 0.;
	d[LOS_F_LOW_FACTOR] = 0.;
	d[LOS_F_HIGH_FACTOR] = 0.;
	d[STAT_RELIABILITY] = 0.;
	d[LENGTH] = 0.;


	return *_platform_tc_status = TC_OKAY;
}

short  DLLEXPORT  VDF_GetLabel(char *label)
{
	strncpy(label, "SANDAG SHRPC04 link-junction-reliability VDF", VDF_LABELSIZE);
	return *_platform_tc_status = TC_OKAY;
}

/**
This is the main part of the vdf function with link and intersection delay
**/

static double TucsonVDF(double Flow, long link, float **Cost) //this is the whole BPR function
{
	double ratio_s, ratio_j, ratio4_s, ratio4_j, lambda_s, lambda_j, val;

	if (Cost[T0][link] == flt_miss || Cost[CAPACITY][link] == flt_miss) // Uncapacitated link, or no previous travel time or no g/c ratio
		val = (double)Cost[T0][link];
	else
	{
		if (Cost[CAPACITY][link] > 0.0) //valid capacity
		{
			ratio_s = (Flow + Cost[PRELOAD][link]) / Cost[CAPACITY][link];//v/c ratio
			if (Cost[GC][link] > 0.0) // valid intersection signal g/c ratio
				ratio_j = (Flow + Cost[PRELOAD][link]) / (Cost[INT_CAPACITY][link]);// revised capacity assumes that it already takes into account G/c Ratio
			else
				ratio_j = BigReal;
		}
		else
			ratio_s = ratio_j = BigReal;

		if (ratio_s >= BigReal)
			ratio4_s = ratio_s;
		else
			ratio4_s = pow(ratio_s, (double)Cost[BETA1][link]);
		lambda_s = 1.0 + Cost[ALPHA1][link] * ratio4_s;
		val = (double)Cost[T0][link] * lambda_s; //segment travel time

		if (ratio_j < BigReal)
		{

			ratio4_j = pow(ratio_j, (double) Cost[BETA2][link]);
			lambda_j = 1.0 + Cost[ALPHA2][link] * ratio4_j;
			val += (Cost[PF][link] * Cost[CYCLE][link] / 2. * pow((double)(1 - Cost[GC][link]), 2.0) * lambda_j); //intersection delay time
		}
	}

	return(val); //return the travel time which is the cost after BPR adjustment for single link
}

/**
This is the SHRP C04 reliability part of the delay function. It returns a factor on travel time based on the v\c ratio of the link.
**/
static double ReliabilityFactor(double Flow, long link, float **Cost)
{

	double ratio_s, reliabilityFactor;
	reliabilityFactor = (double) 0.0;

	if (Cost[T0][link] == flt_miss || Cost[CAPACITY][link] == flt_miss) // Uncapacitated link, or no previous travel time or no g/c ratio
		reliabilityFactor = (double)0.0;
	else
	{
		if (Cost[CAPACITY][link] > 0.0) //valid capacity
		{
			ratio_s = (Flow + Cost[PRELOAD][link]) / Cost[CAPACITY][link];//v/c ratio
    
      //cap reliability effect at 1.5 v/c ratio
      ratio_s = min(ratio_s, 1.5);

			//beta * v\c ratio of link - the v\c threshold + 0.01
			if (ratio_s >= LOS_C_THRESHOLD)
				reliabilityFactor = Cost[LOS_C_FACTOR][link] * ratio_s - LOS_C_THRESHOLD + 0.01;
			if (ratio_s >= LOS_D_THRESHOLD)
				reliabilityFactor += Cost[LOS_D_FACTOR][link] * ratio_s - LOS_D_THRESHOLD + 0.01;
			if (ratio_s >= LOS_E_THRESHOLD)
				reliabilityFactor += Cost[LOS_E_FACTOR][link] * ratio_s - LOS_E_THRESHOLD + 0.01;
			if (ratio_s >= LOS_FL_THRESHOLD)
				reliabilityFactor += Cost[LOS_F_LOW_FACTOR][link] * ratio_s - LOS_FL_THRESHOLD + 0.01;
			if (ratio_s >= LOS_FH_THRESHOLD)
				reliabilityFactor += Cost[LOS_F_HIGH_FACTOR][link] * ratio_s - LOS_FH_THRESHOLD + 0.01;

			//fixed reliability is stored at the link level, and must be added to the result of the above v\c-based factor, and the result is multiplied by link length
			reliabilityFactor = max(reliabilityFactor,0);
			reliabilityFactor = (reliabilityFactor + Cost[STAT_RELIABILITY][link]) * Cost[LENGTH][link];

		}
	}
	return reliabilityFactor;

}

/**
Get the sum of all the relevant reliability factors for calculation of derivative.
**/
static double SumVOCReliabilityFactors(double Flow, long link, float **Cost)
{
	float reliabilityFactor, ratio_s;

  if(Cost[CAPACITY][link] == 0)
  	return(0.);
  	
	reliabilityFactor = (double) 0.0;
	ratio_s = (Flow + Cost[PRELOAD][link]) / Cost[CAPACITY][link];//v/c ratio

    //cap reliability effect at 1.5 v/c ratio
  ratio_s = min(ratio_s, 1.5);

	if (ratio_s >= LOS_C_THRESHOLD)
		reliabilityFactor = Cost[LOS_C_FACTOR][link];
	if (ratio_s >= LOS_D_THRESHOLD)
		reliabilityFactor += Cost[LOS_D_FACTOR][link];
	if (ratio_s >= LOS_E_THRESHOLD)
		reliabilityFactor += Cost[LOS_E_FACTOR][link];
	if (ratio_s >= LOS_FL_THRESHOLD)
		reliabilityFactor += Cost[LOS_F_LOW_FACTOR][link];
	if (ratio_s >= LOS_FH_THRESHOLD)
		reliabilityFactor += Cost[LOS_F_HIGH_FACTOR][link];

	return max(reliabilityFactor,0);
}


double  DLLEXPORT  VDFValue(double *FlowVal, long *link, float **Cost, short *DisabledLinks) //get the VDF value for each individual network link
{
	double val, vdf, reliability;

	*_platform_tc_status = TC_OKAY;

	if (DisabledLinks[*link])
		return(flt_max);

	//the mid-link and intersection time-related penalty
	vdf = TucsonVDF(*FlowVal, *link, Cost);

	//the reliability factor to apply to time
	reliability = ReliabilityFactor(*FlowVal, *link, Cost);

	//the total penalty
	val = vdf + reliability * vdf;

	return(val);
}

void  DLLEXPORT  VDFValues(double *Flow, float **Cost, long *links, short *DisabledLinks) //get the VDF value for a group of network links
{
	long  link;
	float vdf, reliability;

	*_platform_tc_status = TC_OKAY;
	Threshold = 0.;

	for (link = 0; link < *links; link++)
	{
		if (DisabledLinks[link])
			continue;

		if (Cost[T0][link] == flt_miss) //no link travel time
		{
			Cost[CURRENT][link] = flt_miss; //travel time after the VPR adjustment is same as before, invalid
			continue;
		}


		vdf = (float)TucsonVDF(Flow[link], link, Cost);

		reliability = (float)ReliabilityFactor(Flow[link], link, Cost);

		Cost[CURRENT][link] = vdf + reliability * vdf;

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
	long  link;
	float vdf, reliability;

	*_platform_tc_status = TC_OKAY;
	Threshold = 0.;

	for (link = 0; link < *links; link++)
	{
		if (DisabledLinks[link])
			continue;

		if (Cost[T0][link] == flt_miss) //no link travel time
		{
			Cost[CURRENT][link] = flt_miss; //travel time after the VPR adjustment is same as before, invalid
			continue;
		}


		vdf = (float)TucsonVDF(Flow[link], link, Cost);

		Cost[CURRENT][link] = vdf;

		if (Cost[CURRENT][link] < Cost[T0][link])
		{
			*_platform_tc_status = TC_INVINPUT;
			Cost[CURRENT][link] = Cost[T0][link];//make the input is invalid, current travel time is same as the previous one
		}

		Threshold = max(Threshold, Cost[CURRENT][link]);
	}
}

short  DLLEXPORT  VDF_Preprocess(float **Cost, long *links, void *defaults, short *Unused1, short *Unused2)
{
	short s;
	float MaxVector[N_FIELDS - 2], MaxCurrCost;

	s = VDF_Preprocessor(Cost, links, defaults, Unused1, Unused2, &MaxVector[0], &MaxCurrCost);


	Threshold = MaxCurrCost;
	BigReal = pow(flt_max, 1 / MaxVector[BETA1]);

	return(*_platform_tc_status = s);
}
/**
Calculate the derivative of the vdf for a given reliability factor and link function as:
(r (2 a b f c^(-b) x^b+d e (g-1)^2 p y n^(-e) x^e))/(2 x)
where
r = derivative of reliability factor (sum of all factors * link length/capacity)
a = alpha for mid-block
b = beta for mid-block
d = alpha for intersection
e = beta for intersection
c = capacity for mid-block
n = capacity for intersection
x = volume (preload + flow)
p = progression factor
g = gc ratio
y = cycle length
f = free-flow time

**/
static double Reliability_Derivative(double *FlowVal, long *link, float **Cost)
{

	float r, a, b, d, e, f, c, n, x, p, g, y, val;
	val = 0;


	if (Cost[T0][*link] == flt_miss || Cost[CAPACITY][*link] <= 0 || Cost[CAPACITY][*link] == flt_miss ) //if no FFT or capacity or flowval is zero 
		return(0.);
  else if ( (*FlowVal + Cost[PRELOAD][*link])==0)
  	return(0.);
	else
	{
		r = SumVOCReliabilityFactors(*FlowVal, *link, Cost) * Cost[LENGTH][*link] / Cost[CAPACITY][*link];
		
		if(r<=0)
			return(0.);
			
		a = Cost[ALPHA1][*link];
		b = Cost[BETA1][*link];
		d = Cost[ALPHA2][*link];
		e = Cost[BETA2][*link];
		f = Cost[T0][*link];
		c = Cost[CAPACITY][*link];
		n = Cost[INT_CAPACITY][*link];
		x = (*FlowVal + Cost[PRELOAD][*link]);
		p = Cost[PF][*link];
		g = Cost[GC][*link];
		y = Cost[CYCLE][*link];

		val = (r * (2 * a * b * f * pow(c, (-1 * b))* pow(x, b) + d * e * pow((g - 1), 2) * p * y * pow(n, (-1 * e)) * pow(x, e))) / (2 * x);
	}
	return val;
}

// Optional functions
double  DLLEXPORT  VDF_Derivative(double *FlowVal, long *link, float **Cost, short *DisabledLinks)
{
	double pow1_s, pow1_j, vc_s, vc_j, lambda_s, lambda_j, return_var;

	*_platform_tc_status = TC_OKAY;

	if (DisabledLinks[*link])
		return(0.);

	if (Cost[T0][*link] == flt_miss || Cost[CAPACITY][*link] <= 0 || Cost[CAPACITY][*link] == flt_miss) //if no FFT or capacity 
		return(0.);
	else
	{
		//Segment part
		vc_s = (*FlowVal + Cost[PRELOAD][*link]) / Cost[CAPACITY][*link];
		pow1_s = pow(vc_s, Cost[BETA1][*link] - 1.0);
		lambda_s = Cost[ALPHA1][*link] * Cost[BETA1][*link] * pow1_s * (1.0 / Cost[CAPACITY][*link]);
		return_var = (double)Cost[T0][*link] * lambda_s;

		//junction part

		if (Cost[GC][*link] >0)
		{
			vc_j = (*FlowVal + Cost[PRELOAD][*link]) / (Cost[INT_CAPACITY][*link]);
			pow1_j = pow(vc_j, Cost[BETA2][*link] - 1.0);
			lambda_j = Cost[ALPHA2][*link] * Cost[BETA2][*link] * pow1_j * (1.0 / Cost[INT_CAPACITY][*link]);

			return_var += ((double)Cost[PF][*link] * Cost[CYCLE][*link] / 2. * pow((double)(1 - Cost[GC][*link]), 2.0) * lambda_j);
		}

		return_var += Reliability_Derivative(FlowVal, link, Cost);

		return(return_var);
	}
}




double  DLLEXPORT  VDF_Integral(double *FlowVal, long *link, float **Cost, short *DisabledLinks)
{
	double ratio_s, ratio_j, pow1_s, pow1_j, lambda_s, lambda_j, return_var;

	*_platform_tc_status = TC_OKAY;

	if (DisabledLinks[*link])
		return(0.);

	if (Cost[T0][*link] == flt_miss || Cost[CAPACITY][*link] <= 0 || Cost[CAPACITY][*link] == flt_miss || *FlowVal <= 0.)
		return 0.;
	else
	{

		//segment part
		ratio_s = (*FlowVal + Cost[PRELOAD][*link]) / Cost[CAPACITY][*link];
		pow1_s = pow(ratio_s, (double) Cost[BETA1][*link]);
		lambda_s = 1.0 + pow1_s * Cost[ALPHA1][*link] / (Cost[BETA1][*link] + 1.0);
		return_var = (double)Cost[T0][*link] * (*FlowVal + Cost[PRELOAD][*link]) * lambda_s;

		//junction part
		if (Cost[GC][*link] > 0)
		{
			ratio_j = (*FlowVal + Cost[PRELOAD][*link]) * 1.0 / Cost[CAPACITY][*link];
			pow1_j = pow(ratio_j, (double) Cost[BETA2][*link]);
			lambda_j = 1.0 + pow1_j * Cost[ALPHA2][*link] / (Cost[BETA2][*link] + 1.0);
			return_var += (double)Cost[PF][*link] * Cost[CYCLE][*link] / 2. * pow((double)(1 - Cost[GC][*link]), 2.0)*(*FlowVal) * lambda_j;
		}

		return(return_var);
	}
}

