BEGIN{
	
	
# --------------------------------------------------
# User Section

  term=-1
             
   util_params[++term]="p1"
   util_vars[term]="logsm(xxxx)"
   
   util_params[++term]="p2"
   util_vars[term]="wait(xxxx)"

   util_params[++term]="p3"
   util_vars[term]="distance(xxxx)"
      
   util_params[++term]="p4"
   util_vars[term]="distsq(xxxx)"
   
   util_params[++term]="p5"
   util_vars[term]="distcube(xxxx)"
         
   util_params[++term]="p6"
   util_vars[term]="logdist(xxxx)"

   util_params[++term]="p10"
   util_vars[term]="otay(xxxx)"
   
   util_params[++term]="p11"
   util_vars[term]="tec(xxxx)"

   util_params[++term]="p8"
   util_vars[term]="popLog(xxxx)"
   util_terms=term+1
 
   term=-1
   #do not change size_params[0]
   size_params[++term]=""
   size_vars[term]="colEnr(xxxx)"
   
   size_params[++term]="p102"
   size_vars[term]="k12Enr(xxxx)"
   
  
   size_terms=term+1

	inAlts=100
	modes=4
	
	altNumber=0
	for(mode=0;mode<modes;++mode){
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
     if(mode==1)   
     	printf(" + p20\n")
     else if(mode==2)   
     	printf(" + p21\n")
     else if(mode==3)   
     	printf(" + p22\n")

   }
	}
	
	
	#write size terms
  if(size_terms>0){
    printf("\n")
	
	  altNumber=0
	  for(mode=0;mode<modes;++mode){
			for(inAlt=1;inAlt<=inAlts;++inAlt){
				++altNumber
     		printf("SIZE(%i)=",(altNumber))
     		for(j=0;j<size_terms;++j){
        	var = size_vars[j]
        	gsub("xxxx",inAlt,var)
        	if(j>0)
            printf("  %s *",size_params[j])
        	printf(" %s\n",var)
        	if(j<(size_terms-1))
           printf(" +")
     		}
   		}  
		}
	}
}
	
	
	
