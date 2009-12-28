package edu.cs.hku.testSet;

import java.util.HashSet;

public class TestSet {
	public HashSet<TestCase> testCase = null;
	public double coverage = 0.0;
	public int size = 0;
	
	public TestSet(HashSet<TestCase> testCases){
		this(testCases, 0.0, testCases.size());
	}
	
	public TestSet(HashSet<TestCase> testCases, double cov){
		this(testCases, cov, testCases.size());
	}
	
	public TestSet(HashSet<TestCase> testCases, double cov, int size){
		this.testCase = testCases;
		this.coverage = cov;
		this.size = size;
	}
}
