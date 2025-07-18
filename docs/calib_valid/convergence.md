# Convergence

Convergence is used to arrive at a stable solution in the model. This is accomplished using an iterative process in which iterations are compared to convergence criterion, and changes are evaluated against this predefined criterion to determine stability in the solution. 


## Methods 

Each standard ABM3 run contains three global iterations with household sample rates of 25%, 50%, and 100% applied to each iteration, respectively. In the first two iterations, trip demand is expanded to full size using a scale factor before traffic assignment. It additionally uses a specified relative gap, which is a measure of the closeness of the current assignment to a perfect equilibrium assignment as the assignment convergence criterion. This relative gap (RG) in EMME is defined as follows:


$RG = \frac{f(v) - BLB}{f(v)} \times 100$
,

Where $ f(v) $ is the value of the assignment’s objective function at a specific iteration, and $ BLB $ is the best current lower bound $ (LB) $ at that specific iteration. $ LB $ is the solution that the assignment subproblem provides in each iteration for the optimal value of the objective function $ f(v)$. The relative gap is set to 0.0005 in a standard ABM3 run.

Three metrics were used to investigate convergence: link volumes, skims, and trip tables. For each metric, the value changes per iteration for the congested periods of peak morning (AM) and peak afternoon (PM) were compared.


## Conclusions

An analysis of link volume convergence showed that between the third and final iteration, approximately 97% of freeway links had a volume change of less than 5%. In contrast, the proportion of links with volume changes below 5% was lower for other road types: 77% for arterials, 54% for collectors, and 41% for local roads. For skim travel times, changes in the RMSE between iterations 2 and 3 were less than 0.5 across all highway modes, pointing to a very small change between iterations. Travel demand by Time of Day (TOD) and mode were also analyzed by combining highway demand by mode, value of time (VOT) and period across all markets and computing the RMSEs between iterations. Results showed small changes in RMSEs between second and third iterations as shown in Table 1. 

Table 1 RMSE of Iterations 2 and 3 Demand Tables 


<table>
  <tr>
   <td rowspan="3" ><strong>MODE</strong>
   </td>
   <td rowspan="3" ><strong>VOT</strong>
   </td>
   <td colspan="2" ><strong>RMSE</strong>
   </td>
  </tr>
  <tr>
   <td><strong>AM</strong>
   </td>
   <td><strong>PM</strong>
   </td>
  </tr>
  <tr>
   <td>
   </td>
   <td>
   </td>
  </tr>
  <tr>
   <td rowspan="3" ><strong>SOV</strong>
   </td>
   <td><strong>Low</strong>
   </td>
   <td>0.1944
   </td>
   <td>0.2244
   </td>
  </tr>
  <tr>
   <td><strong>Med</strong>
   </td>
   <td>0.1985
   </td>
   <td>0.2284
   </td>
  </tr>
  <tr>
   <td><strong>High</strong>
   </td>
   <td>0.1757
   </td>
   <td>0.2078
   </td>
  </tr>
  <tr>
   <td rowspan="3" ><strong>SR2</strong>
   </td>
   <td><strong>Low</strong>
   </td>
   <td>0.0728
   </td>
   <td>0.0876
   </td>
  </tr>
  <tr>
   <td><strong>Med</strong>
   </td>
   <td>0.0822
   </td>
   <td>0.0875
   </td>
  </tr>
  <tr>
   <td><strong>High</strong>
   </td>
   <td>0.1014
   </td>
   <td>0.1163
   </td>
  </tr>
  <tr>
   <td rowspan="3" ><strong>SR3</strong>
   </td>
   <td><strong>Low</strong>
   </td>
   <td>0.029
   </td>
   <td>0.0439
   </td>
  </tr>
  <tr>
   <td><strong>Med</strong>
   </td>
   <td>0.0365
   </td>
   <td>0.0416
   </td>
  </tr>
  <tr>
   <td><strong>High</strong>
   </td>
   <td>0.0641
   </td>
   <td>0.0788
   </td>
  </tr>
</table>