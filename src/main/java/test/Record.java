package test;

import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;

@CsvDataType()
public class Record {
	//recordID
	String id="";
    @CsvField(pos = 1)
	String path="";

    //dependent variable
    @CsvField(pos = 2)
    int isBuggy=0;

    //independent variable
    //code metrics
    @CsvField(pos = 3)
    int fanIN=0;
    @CsvField(pos = 4)
	int fanOut=0;
    @CsvField(pos = 5)
    int parameters=0;
    @CsvField(pos = 6)
    int localVar=0;
    @CsvField(pos = 7, converterType = DoubleConverter.class)
    double commentRatio=0;
    @CsvField(pos = 8)
    long countPath=0;
    @CsvField(pos = 9)
    int complexity=0;
    @CsvField(pos = 10)
    int execStmt=0;
    @CsvField(pos = 11)
    int maxNesting=0;

    //process metrics
    @CsvField(pos = 12)
    int moduleHistories=0;
    @CsvField(pos = 13)
    int devTotal    = 0;
    @CsvField(pos = 14)
    int devMinor    = 0;
    @CsvField(pos = 15)
    int devMajor    = 0;
    @CsvField(pos = 16, converterType = DoubleConverter.class)
    double ownership   = 0;
    @CsvField(pos = 17)
    int elseAdded=0;
    @CsvField(pos = 18)
    int elseDeleted=0;
    @CsvField(pos = 19)
    int fixChgNum = 0;
    @CsvField(pos = 20)
    int pastBugNum  = 0;
    @CsvField(pos = 21)
    int bugIntroNum = 0;
    @CsvField(pos = 22)
    int logCoupNum  = 0;
    @CsvField(pos = 23)
    int period      = 0;
    @CsvField(pos = 24, converterType = DoubleConverter.class)
    double avgInterval = 0;
    @CsvField(pos = 25)
    int maxInterval = 0;
    @CsvField(pos = 26)
    int minInterval = 0;
    @CsvField(pos = 27)
    int stmtAdded=0;
    @CsvField(pos = 28)
    int maxStmtAdded=0;
    @CsvField(pos = 29, converterType = DoubleConverter.class)
    double avgStmtAdded=0;
    @CsvField(pos = 30)
    int stmtDeleted=0;
    @CsvField(pos = 31)
    int maxStmtDeleted=0;
    @CsvField(pos = 32, converterType = DoubleConverter.class)
    double avgStmtDeleted=0;
    @CsvField(pos = 33)
    int churn=0;
    @CsvField(pos = 34)
    int maxChurn=0;
    @CsvField(pos = 35, converterType = DoubleConverter.class)
    double avgChurn=0;
    @CsvField(pos = 36)
    int decl=0;
    @CsvField(pos = 37)
    int cond=0;
    //@CsvField(pos = 35, converterType = DoubleConverter.class)
    //double hcm = 0;
}