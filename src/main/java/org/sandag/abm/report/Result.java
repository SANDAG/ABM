package org.sandag.abm.report;

import java.util.*;

/**
 * Represents related information so it can be conveniently stored.
 *
 * @author Matt Keating
 *         11/19/2013 
 */
public class Result
{
    private String worksheetName;
    private ArrayList<String> resultLabel = new ArrayList<String>();
    private String criteriaLabel;
    protected ArrayList<ArrayList<Float>> results = new ArrayList<ArrayList<Float>>();
    
    public Result(String worksheetName, String criteriaLabel) {
        this.worksheetName = worksheetName;
        this.criteriaLabel = criteriaLabel;
    }
    
    public String getWorksheetName() {
        return this.worksheetName;
    }

    public void setWorksheetName(String worksheetName) {
        this.worksheetName = worksheetName;
    }
    
    public String getResultLabel(int col) {
        return this.resultLabel.get(col);
    }
    
    public ArrayList<String> getAllResultLabels() {
        return this.resultLabel;
    }

    public void addResultLabel(String resultLabel) {
        this.resultLabel.add(resultLabel);
    }
    
    public String getCriteriaLabel() {
        return this.criteriaLabel;
    }

    public void setCriteriaLabel(String criteriaLabel) {
        this.criteriaLabel = criteriaLabel;
    }
    
    public void addRow() {
        this.results.add(new ArrayList<Float>());
    }
    
    public void addResult(int row, int col, Float val) {
        this.results.get(row).add(col,val);
    }
        
    public Float getResult(int row, int col) {
        return this.results.get(row).get(col);
    } 
    
    public void removeRow(int row){
        this.results.remove(row);
    }    
    
}
