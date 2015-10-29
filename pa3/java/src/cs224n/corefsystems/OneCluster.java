package cs224n.corefsystems;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Mention;
import cs224n.coref.Entity;
import cs224n.util.Pair;

public class OneCluster implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		List<ClusteredMention> clusters = new ArrayList<ClusteredMention>();
    List<Mention> mentions = doc.getMentions();
    if (mentions.size() == 0) {
		  return clusters;
    }
    ClusteredMention newCluster = mentions.get(0).markSingleton();
    for (Mention m: mentions) {
      clusters.add(m.markCoreferent(newCluster));
    }
    return clusters;
	}
}
