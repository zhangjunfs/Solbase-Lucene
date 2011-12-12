package org.apache.lucene.search;

public abstract class EmbeddedFieldFilter extends Filter{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2183037067972585844L;
	protected int fieldNumber;
	
	public EmbeddedFieldFilter(int fieldNumber){
		this.fieldNumber = fieldNumber;
	}
	
	abstract boolean filter(int[] sort);
}
