package cs224n.corefsystems;

import cs224n.coref.*;
import cs224n.util.Pair;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Triple;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;

import java.text.DecimalFormat;
import java.util.*;

import static edu.stanford.nlp.util.logging.Redwood.Util.*;

import java.lang.Math;

/**
 * @author Gabor Angeli (angeli at cs.stanford)
 */
public class ClassifierBased implements CoreferenceSystem {

	private static <E> Set<E> mkSet(E[] array){
		Set<E> rtn = new HashSet<E>();
		Collections.addAll(rtn, array);
		return rtn;
	}

	private static final Set<Object> ACTIVE_FEATURES = mkSet(new Object[]{

			/*
			 * TODO: Create a set of active features
			 */

			Feature.ExactMatch.class,
			Feature.HeadMatch.class,
			Feature.TagMatch.class,
			//Feature.SameSentence.class,
			Feature.Recency.class,
			Feature.SenRecency.class,
			//Feature.PrixIsPronoun.class,
			//Feature.CanIsPronoun.class,
			//Feature.ExistPronoun.class,
			Feature.SameNer.class,
			//Feature.SamePool.class,
			Feature.PronMatch.class,
			Feature.PronNotMatch.class,
			//Feature.PronPSMatch.class,
			//Feature.BothMale.class,
			//Feature.BothFemale.class,
			//Feature.NotBothPronoun.class,
			Feature.GenderAndGenderMatch.class,
			Feature.NumberAndNumberMatch.class,

			//skeleton for how to create a pair feature
			//Pair.make(Feature.IsFeature1.class, Feature.IsFeature2.class),
	});


	private LinearClassifier<Boolean,Feature> classifier;

	public ClassifierBased(){
		StanfordRedwoodConfiguration.setup();
		RedwoodConfiguration.current().collapseApproximate().apply();
	}

	public FeatureExtractor<Pair<Mention,ClusteredMention>,Feature,Boolean> extractor = new FeatureExtractor<Pair<Mention, ClusteredMention>, Feature, Boolean>() {
		private <E> Feature feature(Class<E> clazz, Pair<Mention,ClusteredMention> input, Option<Double> count){
			
			//--Variables
			Mention onPrix = input.getFirst(); //the first mention (referred to as m_i in the handout)
			Mention candidate = input.getSecond().mention; //the second mention (referred to as m_j in the handout)
			Entity candidateCluster = input.getSecond().entity; //the cluster containing the second mention


			//--Features
			/*
			if(clazz.equals(Feature.ExactMatch.class)){
				//(exact string match)
				return new Feature.ExactMatch(onPrix.gloss().equals(candidate.gloss()));*/
//			} else if(clazz.equals(Feature.NewFeature.class) {
				/*
				 * TODO: Add features to return for specific classes. Implement calculating values of features here.
				 */
			//}  else 
			if(clazz.equals(Feature.HeadMatch.class)) {
				//System.out.println(onPrix.gloss() + "||" + onPrix.headWord() + "||" + onPrix.headToken().nerTag());
				return new Feature.HeadMatch(onPrix.headWord().equals(candidate.headWord()));
			
			} else if(clazz.equals(Feature.ExactMatch.class)) {
				return new Feature.ExactMatch(onPrix.gloss().equals(candidate.gloss()));
			} else if(clazz.equals(Feature.TagMatch.class)) {
				return new Feature.ExactMatch(onPrix.headToken().posTag().equals(candidate.headToken().posTag()));
			} else if(clazz.equals(Feature.SameSentence.class)) {
				return new Feature.SameSentence(onPrix.sentence.equals(candidate.sentence));
			} else if(clazz.equals(Feature.Recency.class)) {
				return new Feature.Recency(Math.abs(onPrix.doc.indexOfMention(onPrix) - candidate.doc.indexOfMention(candidate)));
			} else if(clazz.equals(Feature.SenRecency.class)) {
				return new Feature.SenRecency(Math.abs(onPrix.doc.indexOfSentence(onPrix.sentence) - candidate.doc.indexOfSentence(candidate.sentence)));
			} else if(clazz.equals(Feature.PrixIsPronoun.class)) {
				return new Feature.PrixIsPronoun(onPrix.headToken().posTag().equals("PRP"));
			} else if(clazz.equals(Feature.CanIsPronoun.class)) {
				return new Feature.CanIsPronoun(candidate.headToken().posTag().equals("PRP"));
			} else if(clazz.equals(Feature.ExistPronoun.class)) {
				return new Feature.ExistPronoun(onPrix.headToken().posTag().equals("PRP") || candidate.headToken().posTag().equals("PRP"));
			} else if(clazz.equals(Feature.SameNer.class)) {
				return new Feature.SameNer(onPrix.headToken().nerTag().equals(candidate.headToken().nerTag()));
				/*
			} else if(clazz.equals(Feature.SamePool.class)) {
				String onPrixPool = onPrix.headToken().pool();
				String candidatePool = candidate.headToken().pool();
				return new Feature.SamePool(onPrixPool.equals(candidatePool) || onPrixPool.equals("none") || candidatePool.equals("none"));
				*/
			} else if(clazz.equals(Feature.PronMatch.class)) {
				return new Feature.PronMatch(onPrix.headToken().posTag().equals("PRP") && candidate.headToken().posTag().equals("PRP") && onPrix.headToken().gender().equals(candidate.headToken().gender()) && onPrix.headToken().ps().equals(candidate.headToken().ps()));
			} else if(clazz.equals(Feature.PronNotMatch.class)) {
				return new Feature.PronNotMatch(onPrix.headToken().posTag().equals("PRP") && candidate.headToken().posTag().equals("PRP") && (!onPrix.headToken().gender().equals(candidate.headToken().gender()) || !onPrix.headToken().ps().equals(candidate.headToken().ps())));
			} else if(clazz.equals(Feature.PronPSMatch.class)) {
				return new Feature.PronPSMatch(onPrix.headToken().posTag().equals("PRP") && candidate.headToken().posTag().equals("PRP") && onPrix.headToken().ps().equals(candidate.headToken().ps()));
			} else if(clazz.equals(Feature.BothMale.class)) {
				return new Feature.BothMale(onPrix.headToken().posTag().equals("PRP") && candidate.headToken().posTag().equals("PRP") && onPrix.headToken().gender().equals("male") && candidate.headToken().ps().equals("male"));
			} else if(clazz.equals(Feature.BothFemale.class)) {
				return new Feature.BothFemale(onPrix.headToken().posTag().equals("PRP") && candidate.headToken().posTag().equals("PRP") && onPrix.headToken().gender().equals("female") && candidate.headToken().ps().equals("female"));
			}	else if(clazz.equals(Feature.NotBothPronoun.class)) {
				return new Feature.NumberAndNumberMatch(!(Pronoun.isSomePronoun(onPrix.headWord()) && Pronoun.isSomePronoun(candidate.headWord())));
			}	else if(clazz.equals(Feature.GenderAndGenderMatch.class)) {
				Util ut = new Util();
				Pair<Boolean, Boolean> gg = Util.haveGenderAndAreSameGender(onPrix, candidate);
				return new Feature.GenderAndGenderMatch(gg.getFirst() && gg.getSecond());
			}	else if(clazz.equals(Feature.NumberAndNumberMatch.class)) {
				Util ut = new Util();
				Pair<Boolean, Boolean> nn = Util.haveNumberAndAreSameNumber(onPrix, candidate);
				return new Feature.NumberAndNumberMatch(nn.getFirst() && nn.getSecond());
			}
			else {
				throw new IllegalArgumentException("Unregistered feature: " + clazz);
			}
		}

		@SuppressWarnings({"unchecked"})
		@Override
		protected void fillFeatures(Pair<Mention, ClusteredMention> input, Counter<Feature> inFeatures, Boolean output, Counter<Feature> outFeatures) {
			//--Input Features
			for(Object o : ACTIVE_FEATURES){
				if(o instanceof Class){
					//(case: singleton feature)
					Option<Double> count = new Option<Double>(1.0);
					Feature feat = feature((Class) o, input, count);
					if(count.get() > 0.0){
						inFeatures.incrementCount(feat, count.get());
					}
				} else if(o instanceof Pair){
					//(case: pair of features)
					Pair<Class,Class> pair = (Pair<Class,Class>) o;
					Option<Double> countA = new Option<Double>(1.0);
					Option<Double> countB = new Option<Double>(1.0);
					Feature featA = feature(pair.getFirst(), input, countA);
					Feature featB = feature(pair.getSecond(), input, countB);
					if(countA.get() * countB.get() > 0.0){
						inFeatures.incrementCount(new Feature.PairFeature(featA, featB), countA.get() * countB.get());
					}
				}
			}

			//--Output Features
			if(output != null){
				outFeatures.incrementCount(new Feature.CoreferentIndicator(output), 1.0);
			}
		}

		@Override
		protected Feature concat(Feature a, Feature b) {
			return new Feature.PairFeature(a,b);
		}
	};

	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		startTrack("Training");
		//--Variables
		RVFDataset<Boolean, Feature> dataset = new RVFDataset<Boolean, Feature>();
		LinearClassifierFactory<Boolean, Feature> fact = new LinearClassifierFactory<Boolean,Feature>();
		//--Feature Extraction
		startTrack("Feature Extraction");
		for(Pair<Document,List<Entity>> datum : trainingData){
			//(document variables)
			Document doc = datum.getFirst();
			List<Entity> goldClusters = datum.getSecond();
			List<Mention> mentions = doc.getMentions();
			Map<Mention,Entity> goldEntities = Entity.mentionToEntityMap(goldClusters);
			startTrack("Document " + doc.id);
			//(for each mention...)
			for(int i=0; i<mentions.size(); i++){
				//(get the mention and its cluster)
				Mention onPrix = mentions.get(i);
				Entity source = goldEntities.get(onPrix);
				if(source == null){ throw new IllegalArgumentException("Mention has no gold entity: " + onPrix); }
				//(for each previous mention...)
				int oldSize = dataset.size();
				for(int j=i-1; j>=0; j--){
					//(get previous mention and its cluster)
					Mention cand = mentions.get(j);
					Entity target = goldEntities.get(cand);
					if(target == null){ throw new IllegalArgumentException("Mention has no gold entity: " + cand); }
					//(extract features)
					Counter<Feature> feats = extractor.extractFeatures(Pair.make(onPrix, cand.markCoreferent(target)));
					//(add datum)
					dataset.add(new RVFDatum<Boolean, Feature>(feats, target == source));
					//(stop if
					if(target == source){ break; }
				}
				//logf("Mention %s (%d datums)", onPrix.toString(), dataset.size() - oldSize);
			}
			endTrack("Document " + doc.id);
		}
		endTrack("Feature Extraction");
		//--Train Classifier
		startTrack("Minimizer");
		this.classifier = fact.trainClassifier(dataset);
		endTrack("Minimizer");
		//--Dump Weights
		startTrack("Features");
		//(get labels to print)
		Set<Boolean> labels = new HashSet<Boolean>();
		labels.add(true);
		//(print features)
		for(Triple<Feature,Boolean,Double> featureInfo : this.classifier.getTopFeatures(labels, 0.0, true, 100, true)){
			Feature feature = featureInfo.first();
			Boolean label = featureInfo.second();
			Double magnitude = featureInfo.third();
			//log(FORCE,new DecimalFormat("0.000").format(magnitude) + " [" + label + "] " + feature);
		}
		end_Track("Features");
		endTrack("Training");
	}

	public List<ClusteredMention> runCoreference(Document doc) {
		//--Overhead
		startTrack("Testing " + doc.id);
		//(variables)
		List<ClusteredMention> rtn = new ArrayList<ClusteredMention>(doc.getMentions().size());
		List<Mention> mentions = doc.getMentions();
		int singletons = 0;
		//--Run Classifier
		for(int i=0; i<mentions.size(); i++){
			//(variables)
			Mention onPrix = mentions.get(i);
			int coreferentWith = -1;
			//(get mention it is coreferent with)
			for(int j=i-1; j>=0; j--){

				ClusteredMention cand = rtn.get(j);
				
				boolean coreferent = classifier.classOf(new RVFDatum<Boolean, Feature>(
						       extractor.extractFeatures(Pair.make(onPrix, cand))));
				
				if(coreferent){
					coreferentWith = j;
					break;
				}
			}

			if(coreferentWith < 0){
				singletons += 1;
				rtn.add(onPrix.markSingleton());
			} else {
				//log("Mention " + onPrix + " coreferent with " + mentions.get(coreferentWith));
				rtn.add(onPrix.markCoreferent(rtn.get(coreferentWith)));
			}
		}
		//log("" + singletons + " singletons");
		//--Return
		endTrack("Testing " + doc.id);
		return rtn;
	}

	private class Option<T> {
		private T obj;
		public Option(T obj){ this.obj = obj; }
		public Option(){};
		public T get(){ return obj; }
		public void set(T obj){ this.obj = obj; }
		public boolean exists(){ return obj != null; }
	}
}
