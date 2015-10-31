package cs224n.corefsystems;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;


import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.*;
import cs224n.util.Pair;
import cs224n.util.CounterMap;

public class BetterBaseline implements CoreferenceSystem { 
  private CounterMap<String, String> headStats;

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
    this.headStats = new CounterMap<String, String>();
	  for(Pair<Document, List<Entity>> pair : trainingData){
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();

      for (Entity e: clusters) {
        for (Pair<Mention, Mention> p: e.orderedMentionPairs()) {
          String head1 = p.getFirst().headWord();
          String head2 = p.getSecond().headWord();
          this.headStats.incrementCount(head1, head2, 1);
          this.headStats.incrementCount(head2, head1, 1);
        }
      }
    }
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
  	ArrayList<ClusteredMention> taggedMentions = new ArrayList<ClusteredMention>();
    List<Mention> mentions = doc.getMentions();
    for (int i = 0; i < mentions.size(); i++) {
      double maxProb = 0.1;
      ClusteredMention bestCluster = null;
      for (int j = 0; j < i; j++) {
        double currProb = this.headStats.getCount(
          mentions.get(i).headWord(),
          mentions.get(j).headWord()
        );
        if (currProb >= maxProb) { // use bigger equal to assign to nearby cluster
          maxProb = currProb;
          bestCluster = taggedMentions.get(j);
        } 
      }
      if (bestCluster != null) {
        taggedMentions.add(mentions.get(i).markCoreferent(bestCluster));
      } else {
        taggedMentions.add(mentions.get(i).markSingleton());
      }
    }
    return taggedMentions;
  }
}
