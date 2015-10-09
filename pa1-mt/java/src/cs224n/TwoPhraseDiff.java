package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;

import edu.stanford.nlp.mt.decoder.feat.DerivationFeaturizer;
import edu.stanford.nlp.mt.decoder.feat.FeaturizerState;
import edu.stanford.nlp.mt.tm.ConcreteRule;
import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.util.Sequence;
import edu.stanford.nlp.util.Generics;

import java.lang.Math;

/**
 * Signed discriminative distortion bins. (see <code>ConcreteRule</code>)
 * 
 * @author Spence Green
 *
 */
public class TwoPhraseDiff extends DerivationFeaturizer<IString, String> {

  @Override
  public void initialize(int sourceInputId,
      List<ConcreteRule<IString, String>> ruleList, Sequence<IString> source) {
  }

  @Override
  public List<FeatureValue<String>> featurize(Featurizable<IString, String> f) {

	  int TwoPhraseDiff = f.targetPhrase.size() + f.prior.targetPhrase.size()
												- f.sourcePhrase.size() - f.prior.sourcePhrase.size();
    //double absoluteDistortion = Math.log1p(Math.abs(TwoPhraseDiff));
		
    double absoluteDistortion = Math.abs(TwoPhraseDiff);

    List<FeatureValue<String>> features = Generics.newLinkedList();

    features.add(new FeatureValue<String>("AbsoluteDistortion", absoluteDistortion));
    return features;
  }  
}
