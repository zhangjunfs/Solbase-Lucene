package org.apache.lucene.search;

public class EmbeddedTermFieldFilter extends EmbeddedFieldFilter{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3084124907459368644L;

	private int val;
	
	public EmbeddedTermFieldFilter(int val, int fieldNumber) {
		super(fieldNumber);
		this.val = val;
	}

	public boolean filter(int[] sort){
		if(sort[fieldNumber-1] == -1 || val == -1){
			// if fieldNumber is negative, it is not set, so do not compare
			return true;
		}
		return sort[fieldNumber-1] == val;
	}
}
