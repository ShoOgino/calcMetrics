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
	int fanOut=0;
    @CsvField(pos = 4)
    int parameters=0;
    @CsvField(pos = 5)
    int localVar=0;
    @CsvField(pos = 6, converterType = DoubleConverter.class)
    double commentRatio=0;
    @CsvField(pos = 7)
    long countPath=0;
    @CsvField(pos = 8)
    int complexity=0;
    @CsvField(pos = 9)
    int execStmt=0;
    @CsvField(pos = 10)
    int maxNesting=0;

    //process metrics
    @CsvField(pos = 11)
    int moduleHistories=0;
    @CsvField(pos = 12)
    int authors    = 0;
    @CsvField(pos = 13)
    int stmtAdded=0;
    @CsvField(pos = 14)
    int maxStmtAdded=0;
    @CsvField(pos = 15, converterType = DoubleConverter.class)
    double avgStmtAdded=0;
    @CsvField(pos = 16)
    int stmtDeleted=0;
    @CsvField(pos = 17)
    int maxStmtDeleted=0;
    @CsvField(pos = 18, converterType = DoubleConverter.class)
    double avgStmtDeleted=0;
    @CsvField(pos = 19)
    int churn=0;
    @CsvField(pos = 20)
    int maxChurn=0;
    @CsvField(pos = 21, converterType = DoubleConverter.class)
    double avgChurn=0;
    @CsvField(pos = 22)
    int decl=0;
    @CsvField(pos = 23)
    int cond=0;
    @CsvField(pos = 24)
    int elseAdded=0;
    @CsvField(pos = 25)
    int elseDeleted=0;
}