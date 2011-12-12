package org.apache.lucene.search;

import java.util.ArrayList;
import java.util.List;

public class EmbeddedMultiFieldsFilter extends EmbeddedFieldFilter{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8428395833255037975L;
	List<EmbeddedFieldFilter> filters = new ArrayList<EmbeddedFieldFilter>();
	
	public EmbeddedMultiFieldsFilter() {
		// it doesn't care about fieldNumber in this obj. 
		super(-1);
	}
	
	public void addFilter(EmbeddedFieldFilter filter){
		this.filters.add(filter);
	}

	public boolean filter(int sort[]){
		for(EmbeddedFieldFilter filter: this.filters){
			if(!filter.filter(sort)){
				return false;
			}
		}
		return true;
	}
	
	public boolean isEmptyFilter(){
		return filters.size() == 0;
	}
}
