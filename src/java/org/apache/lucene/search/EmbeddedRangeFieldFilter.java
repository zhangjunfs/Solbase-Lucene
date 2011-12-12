package org.apache.lucene.search;

public class EmbeddedRangeFieldFilter extends EmbeddedFieldFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8320062346977576099L;
	protected int start;
	protected int end;
	
	public EmbeddedRangeFieldFilter(int start, int end, int fieldNumber){
		super(fieldNumber);
		this.start = start;
		this.end = end;
	}
	@Override
	boolean filter(int[] sort) {
		int sortVal = sort[this.fieldNumber - 1];
		
		return sortVal >= start && sortVal <= end;
	}

}
