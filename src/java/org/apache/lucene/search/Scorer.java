package org.apache.lucene.search;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Expert: Common scoring functionality for different types of queries.
 *
 * <p>
 * A <code>Scorer</code> iterates over documents matching a
 * query in increasing order of doc Id.
 * </p>
 * <p>
 * Document scores are computed using a given <code>Similarity</code>
 * implementation.
 * </p>
 *
 * <p><b>NOTE</b>: The values Float.Nan,
 * Float.NEGATIVE_INFINITY and Float.POSITIVE_INFINITY are
 * not valid scores.  Certain collectors (eg {@link
 * TopScoreDocCollector}) will not properly collect hits
 * with these scores.
 *
 * @see BooleanQuery#setAllowDocsOutOfOrder
 */
public abstract class Scorer extends DocIdSetIterator {
	// TODO: may want to read number of sort fields straight out from schema.xml instead of setting property 
	public static int numSort = 5; // set default to be 5 sortable fields
	
	// used to save bytes on current time minutes (2005/1/1)
	protected static long SolbaseEpochTime = 18408960l;
	
	// flag to use timeliness boosting factor.
	protected static boolean enableTimelinessBoosting = false;
	
	// field position for timeliness boosting
	protected static int timelinessFieldPosition;
	
	// boosting factor
	protected static int boostingFactor = 10080; // 24 * 60 * 7 doing seven days boosting in minutes;
	
	// timeliness boosting factor
	protected static float boost = 5.0f;
	
	static {
		String numSortsStr = System.getProperty("lucene.sortablefield.num");
		try {
			ResourceBundle luceneBundle = ResourceBundle.getBundle("lucene");
			if (numSortsStr == null && luceneBundle != null) {
				try {
					numSortsStr = luceneBundle.getString("sortablefield.num");
					try {
						numSort = Integer.parseInt(numSortsStr);
					} catch (NumberFormatException e) {
						// use default which is already set above
					}
				}catch (java.util.MissingResourceException e){	
					// use default
				}
			}
			// initialize SolbaseEpochTime
			try {
				String solbaseEpochTimeStr = luceneBundle.getString("solbaseEpochTime");
				try {
					SolbaseEpochTime = Long.parseLong(solbaseEpochTimeStr);
				} catch (NumberFormatException e) {
					// use default
				}
			}catch (java.util.MissingResourceException e){	
				// use default
			}
			
			// init enableTimelinessBoosting flag
			try {
				String timelinessBoosting = luceneBundle.getString("timelinessBoosting");
				if(timelinessBoosting.equals("true")) {
					// make sure we have a field to get value for boosting comparison
					try {
						String fieldStr = luceneBundle.getString("timelinessBoosting.field.position");
						timelinessFieldPosition = Integer.parseInt(fieldStr);
						enableTimelinessBoosting = true;
					} catch (NumberFormatException e) {
						// can't get field postiion for timeliness boosting
						// suppress timeliness boosting feature
					} catch (java.util.MissingResourceException e) {
						// use default
					}
					// init boosting val
					try {
						String boostStr = luceneBundle.getString("timelinessBoosting.boost");
						boost = Float.parseFloat(boostStr);
					} catch (NumberFormatException e) {
						// use default 5.0f boost
					} catch (java.util.MissingResourceException e) {
						// use default
					}
					// init boosting factor
					try {
						String boostingFactorStr = luceneBundle.getString("timelinessBoosting.boostingFactor");
						boostingFactor = Integer.parseInt(boostingFactorStr);
					} catch (NumberFormatException e) {
						// use default 7 days in seconds.
					} catch (java.util.MissingResourceException e) {
						// use default
					}
				}
			} catch (java.util.MissingResourceException e){	
				// use default
			}
		} catch (java.util.MissingResourceException e){
			// ignore if resouce is not here
		}
	}
	
  private Similarity similarity;
	
  /** Constructs a Scorer.
   * @param similarity The <code>Similarity</code> implementation used by this scorer.
   */
  protected Scorer(Similarity similarity) {
    this.similarity = similarity;
  }

  /** Returns the Similarity implementation used by this scorer. */
  public Similarity getSimilarity() {
    return this.similarity;
  }

  /** Scores and collects all matching documents.
   * @param hc The collector to which all matching documents are passed through
   * {@link HitCollector#collect(int, float)}.
   * <br>When this method is used the {@link #explain(int)} method should not be used.
   * @deprecated use {@link #score(Collector)} instead.
   */
  public void score(HitCollector hc) throws IOException {
    score(new HitCollectorWrapper(hc));
  }
  
  /** Scores and collects all matching documents.
   * @param collector The collector to which all matching documents are passed.
   * <br>When this method is used the {@link #explain(int)} method should not be used.
   */
  public void score(Collector collector) throws IOException {
    collector.setScorer(this);
    int doc;
    while ((doc = nextDoc()) != NO_MORE_DOCS) {
      collector.collect(doc, getSorts());
    }
  }

  /** Expert: Collects matching documents in a range.  Hook for optimization.
   * Note that {@link #next()} must be called once before this method is called
   * for the first time.
   * @param hc The collector to which all matching documents are passed through
   * {@link HitCollector#collect(int, float)}.
   * @param max Do not score documents past this.
   * @return true if more matching documents may remain.
   * @deprecated use {@link #score(Collector, int, int)} instead.
   */
  protected boolean score(HitCollector hc, int max) throws IOException {
    return score(new HitCollectorWrapper(hc), max, docID());
  }
  
  /**
   * Expert: Collects matching documents in a range. Hook for optimization.
   * Note, <code>firstDocID</code> is added to ensure that {@link #nextDoc()}
   * was called before this method.
   * 
   * @param collector
   *          The collector to which all matching documents are passed.
   * @param max
   *          Do not score documents past this.
   * @param firstDocID
   *          The first document ID (ensures {@link #nextDoc()} is called before
   *          this method.
   * @return true if more matching documents may remain.
   */
  protected boolean score(Collector collector, int max, int firstDocID) throws IOException {
    collector.setScorer(this);
    int doc = firstDocID;
    while (doc < max) {
      collector.collect(doc, getSorts());
      doc = nextDoc();
    }
    return doc != NO_MORE_DOCS;
  }
  
  /** Returns the score of the current document matching the query.
   * Initially invalid, until {@link #next()} or {@link #skipTo(int)}
   * is called the first time, or when called from within
   * {@link Collector#collect}.
   */
  public abstract float score() throws IOException;

  /** Returns an explanation of the score for a document.
   * <br>When this method is used, the {@link #next()}, {@link #skipTo(int)} and
   * {@link #score(HitCollector)} methods should not be used.
   * @param doc The document number for the explanation.
   *
   * @deprecated Please use {@link IndexSearcher#explain}
   * or {@link Weight#explain} instead.
   */
  public Explanation explain(int doc) throws IOException {
    throw new UnsupportedOperationException();
  }

}
