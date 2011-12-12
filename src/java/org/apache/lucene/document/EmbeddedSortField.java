package org.apache.lucene.document;

import java.io.Reader;
import java.io.Serializable;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.util.StringHelper;

public class EmbeddedSortField extends AbstractField implements Fieldable, Serializable {

	private static final long serialVersionUID = 1L;
	
	private int sortSlot;
	
	public int getSortSlot(){
		return sortSlot;
	}
	
	/**
	 * The value of the field as a String, or null. If null, the Reader value or
	 * binary value is used. Exactly one of stringValue(), readerValue(), and
	 * getBinaryValue() must be set.
	 */
	public String stringValue() {
		return fieldsData instanceof String ? (String) fieldsData : null;
	}

	/**
	 * The value of the field as a Reader, or null. If null, the String value or
	 * binary value is used. Exactly one of stringValue(), readerValue(), and
	 * getBinaryValue() must be set.
	 */
	public Reader readerValue() {
		return fieldsData instanceof Reader ? (Reader) fieldsData : null;
	}

	/**
	 * The value of the field in Binary, or null. If null, the Reader value, or
	 * String value is used. Exactly one of stringValue(), readerValue(), and
	 * getBinaryValue() must be set.
	 * 
	 * @deprecated This method must allocate a new byte[] if the
	 *             {@link AbstractField#getBinaryOffset()} is non-zero or
	 *             {@link AbstractField#getBinaryLength()} is not the full
	 *             length of the byte[]. Please use
	 *             {@link AbstractField#getBinaryValue()} instead, which simply
	 *             returns the byte[].
	 */
	public byte[] binaryValue() {
		if (!isBinary)
			return null;
		final byte[] data = (byte[]) fieldsData;
		if (binaryOffset == 0 && data.length == binaryLength)
			return data; // Optimization

		final byte[] ret = new byte[binaryLength];
		System.arraycopy(data, binaryOffset, ret, 0, binaryLength);
		return ret;
	}

	/**
	 * The TokesStream for this field to be used when indexing, or null. If
	 * null, the Reader value or String value is analyzed to produce the indexed
	 * tokens.
	 */
	public TokenStream tokenStreamValue() {
		return tokenStream;
	}

	/**
	 * Create a field by specifying its name, value and how it will be saved in
	 * the index. Term vectors will not be stored in the index.
	 * 
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The string to process
	 * @param store
	 *            Whether <code>value</code> should be stored in the index
	 * @param index
	 *            Whether the field should be indexed, and if so, if it should
	 *            be tokenized before indexing
	 * @param sortSlotNum
	 * 
	 * @throws NullPointerException
	 *             if name or value is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the field is neither stored nor indexed
	 */
	
	public EmbeddedSortField(String name, String value, Store store, Index index, int sortSlot) {
		this(name, value, store, index, TermVector.NO);
		this.sortSlot = sortSlot;
	}
	/**
	 * Create a field by specifying its name, value and how it will be saved in
	 * the index.
	 * 
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The string to process
	 * @param store
	 *            Whether <code>value</code> should be stored in the index
	 * @param index
	 *            Whether the field should be indexed, and if so, if it should
	 *            be tokenized before indexing
	 * @param termVector
	 *            Whether term vector should be stored
	 * @throws NullPointerException
	 *             if name or value is <code>null</code>
	 * @throws IllegalArgumentException
	 *             in any of the following situations:
	 *             <ul>
	 *             <li>the field is neither stored nor indexed</li>
	 *             <li>the field is not indexed but termVector is
	 *             <code>TermVector.YES</code></li>
	 *             </ul>
	 */
	public EmbeddedSortField(String name, String value, Store store, Index index, TermVector termVector) {
		this(name, true, value, store, index, termVector);
	}

	/**
	 * Create a field by specifying its name, value and how it will be saved in
	 * the index.
	 * 
	 * @param name
	 *            The name of the field
	 * @param internName
	 *            Whether to .intern() name or not
	 * @param value
	 *            The string to process
	 * @param store
	 *            Whether <code>value</code> should be stored in the index
	 * @param index
	 *            Whether the field should be indexed, and if so, if it should
	 *            be tokenized before indexing
	 * @param termVector
	 *            Whether term vector should be stored
	 * @throws NullPointerException
	 *             if name or value is <code>null</code>
	 * @throws IllegalArgumentException
	 *             in any of the following situations:
	 *             <ul>
	 *             <li>the field is neither stored nor indexed</li>
	 *             <li>the field is not indexed but termVector is
	 *             <code>TermVector.YES</code></li>
	 *             </ul>
	 */
	@SuppressWarnings("deprecation")
	public EmbeddedSortField(String name, boolean internName, String value, Store store, Index index, TermVector termVector) {
		if (name == null)
			throw new NullPointerException("name cannot be null");
		if (value == null)
			throw new NullPointerException("value cannot be null");
		if (name.length() == 0 && value.length() == 0)
			throw new IllegalArgumentException("name and value cannot both be empty");
		if (index == Index.NO && store == Store.NO)
			throw new IllegalArgumentException("it doesn't make sense to have a field that " + "is neither indexed nor stored");
		if (index == Index.NO && termVector != TermVector.NO)
			throw new IllegalArgumentException("cannot store term vector information " + "for a field that is not indexed");

		if (internName) // field names are optionally interned
			name = StringHelper.intern(name);

		this.name = name;

		this.fieldsData = value;

		if (store == Store.YES) {
			this.isStored = true;
			this.isCompressed = false;
		} else if (store == Store.COMPRESS) {
			this.isStored = true;
			this.isCompressed = true;
		} else if (store == Store.NO) {
			this.isStored = false;
			this.isCompressed = false;
		} else
			throw new IllegalArgumentException("unknown store parameter " + store);

		if (index == Index.NO) {
			this.isIndexed = false;
			this.isTokenized = false;
			this.omitTermFreqAndPositions = false;
			this.omitNorms = true;
		} else if (index == Index.ANALYZED) {
			this.isIndexed = true;
			this.isTokenized = true;
		} else if (index == Index.NOT_ANALYZED) {
			this.isIndexed = true;
			this.isTokenized = false;
		} else if (index == Index.NOT_ANALYZED_NO_NORMS) {
			this.isIndexed = true;
			this.isTokenized = false;
			this.omitNorms = true;
		} else if (index == Index.ANALYZED_NO_NORMS) {
			this.isIndexed = true;
			this.isTokenized = true;
			this.omitNorms = true;
		} else {
			throw new IllegalArgumentException("unknown index parameter " + index);
		}

		this.isBinary = false;

		setStoreTermVector(termVector);
	}
	
	/**
	 * <p>
	 * Expert: change the value of this field. This can be used during indexing
	 * to re-use a single Field instance to improve indexing speed by avoiding
	 * GC cost of new'ing and reclaiming Field instances. Typically a single
	 * {@link Document} instance is re-used as well. This helps most on small
	 * documents.
	 * </p>
	 * 
	 * <p>
	 * Each Field instance should only be used once within a single
	 * {@link Document} instance. See <a
	 * href="http://wiki.apache.org/lucene-java/ImproveIndexingSpeed"
	 * >ImproveIndexingSpeed</a> for details.
	 * </p>
	 */
	public void setValue(String value) {
		if (isBinary) {
			throw new IllegalArgumentException("cannot set a String value on a binary field");
		}
		fieldsData = value;
	}
}
