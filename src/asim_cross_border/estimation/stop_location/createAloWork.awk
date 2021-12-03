BEGIN{
	
	
# --------------------------------------------------
# User Section

  term=-1
             
   util_params[++term]="p1"
   util_vars[term]="lgsum(xxxx)"
   
   util_params[++term]="p2"
   util_vars[term]="osDist(xxxx)"
      
   util_params[++term]="p3"
   util_vars[term]="sdDist(xxxx)"
   
   util_params[++term]="p4"
   util_vars[term]="totDist(xxxx)"
   
   util_params[++term]="p5"
   util_vars[term]="osDstfolo(xxxx)"
   
   util_params[++term]="p6"
   util_vars[term]="sdDstfolo(xxxx)" 
                   
   util_params[++term]="p7"
   util_vars[term]="osDstfoli(xxxx)"                 
   
   util_params[++term]="p8"
   util_vars[term]="sdDstfoli(xxxx)"                 
   
   util_params[++term]="p9"
   util_vars[term]="osDstlolo(xxxx)"                 
   
   util_params[++term]="p10"
   util_vars[term]="sdDstlolo(xxxx)"                 
   
   util_params[++term]="p11"
   util_vars[term]="osDstloli(xxxx)"                 
   
   util_params[++term]="p12"
   util_vars[term]="sdDstloli(xxxx)"   
   
   util_params[++term]="p15"
   util_vars[term]="distOsPed(xxxx)"                 
   
   util_params[++term]="p15"
   util_vars[term]="distSdPed(xxxx)"   
      
   util_params[++term]="p16"
   util_vars[term]="osDisto(xxxx)"   
 
    util_params[++term]="p17"
   util_vars[term]="sdDisto(xxxx)"   
 
    util_params[++term]="p18"
   util_vars[term]="osDisti(xxxx)"   
 
    util_params[++term]="p19"
   util_vars[term]="sdDisti(xxxx)"   
 
    util_params[++term]="p20"
   util_vars[term]="dist0_2hr(xxxx)"   
 
    util_params[++term]="p21"
   util_vars[term]="dist2_4hr(xxxx)"   

    util_params[++term]="p22"
   util_vars[term]="dist8phr(xxxx)"   
 
   util_params[++term]="p101"
   util_vars[term]="logSize(xxxx)"
   util_terms=term+1
 
#   term=-1
#
#   #do not change size_params[0]
#   size_params[++term]=""
#   size_vars[term]="hhs(xxxx)"
#   
##   size_params[++term]="p102"
##   size_vars[term]="agEmp(xxxx)"
#   
#   size_params[++term]="p103"
#   size_vars[term]="consEmp(xxxx)"
#    
#   size_params[++term]="p104"
#   size_vars[term]="offEmp(xxxx)"
#   
#   size_params[++term]="p105"
#   size_vars[term]="retEmp(xxxx)"
#   
#   size_params[++term]="p106"
#   size_vars[term]="ahrEmp(xxxx)"
#   
#   size_params[++term]="p107"
#   size_vars[term]="othEmp(xxxx)"
#   size_terms=term+1
#
	inAlts=500
	
	altNumber=0
		for(inAlt=1;inAlt<=inAlts;++inAlt){
			++altNumber
	
	   printf("UTIL(%i)=",altNumber)
     for(j=0;j<util_terms;++j){
        var = util_vars[j]
        gsub("xxxx",altNumber,var)
        if(util_params[j]=="")
           printf("  %s\n",var)
        else
          printf("  %s * %s\n",util_params[j],var)
        if(j<(util_terms-1))
           printf(" +")
           
        	
     }

   }
	
	
#	#write size terms
#  if(size_terms>0){
#    printf("\n")
#	
#	  altNumber=0
#	  for(mode=0;mode<modes;++mode){
#			for(inAlt=1;inAlt<=inAlts;++inAlt){
#				++altNumber
#     		printf("SIZE(%i)=",(altNumber))
#     		for(j=0;j<size_terms;++j){
#        	var = size_vars[j]
#        	gsub("xxxx",inAlt,var)
#        	if(j>0)
#            printf("  %s *",size_params[j])
#        	printf(" %s\n",var)
#        	if(j<(size_terms-1))
#           printf(" +")
#     		}
#   		}  
#		}
#	}
}
	
	
	
