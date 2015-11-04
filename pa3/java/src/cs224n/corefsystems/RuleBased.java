package cs224n.corefsystems;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Mention;
import cs224n.coref.Sentence;
import cs224n.coref.Entity;
import cs224n.util.Pair;
import java.util.ArrayList;
import java.util.*;

public class RuleBased implements CoreferenceSystem {
	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
		// turn all mentions to singleton
    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
    for (Mention m: doc.getMentions()) {
      mentions.add(m.markSingleton());
    }
    ExactMatchSieve exactMatchSieve = new ExactMatchSieve();
    StrictHeadMatchingSieve strictHeadMatchingSeive = new StrictHeadMatchingSieve();
    SingletonSieve singletonSieve = new SingletonSieve();
    return passAllSieves(mentions, strictHeadMatchingSeive);
	}

  public List<ClusteredMention> passAllSieves(
    List<ClusteredMention> mentions, 
    Sieve... s
  ) {
    if (s.length == 0) {
      return mentions;
    } else {
      return passAllSieves(
        s[0].passSieve(mentions), 
        Arrays.copyOfRange(s, 1, s.length)
      );
    }
  }

  public static interface Sieve {
    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions);
  }

  public class SingletonSieve implements Sieve {
    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();
      for (ClusteredMention cmention: mentions) {
        Mention m = cmention.mention;
        m.removeCoreference();
        newMentions.add(m.markSingleton());
      }
      return newMentions;
    }
  }

  public class ExactMatchSieve implements Sieve {
    // TODO Exclude pronoun
    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();
      int n = mentions.size();
      if (n == 0) return mentions;

      for (int i = 0; i < n; i++) {
        Mention curr_men = mentions.get(i).mention;
        curr_men.removeCoreference();
        for (int j = i - 1; j > -1; j--) {
          Mention prev_men = newMentions.get(j).mention;
          if (curr_men.parse.equals(prev_men.parse)) {
            newMentions.add(curr_men.markCoreferent(newMentions.get(j)));
            break;
          }
        }
        if (newMentions.size() < i + 1) {
          newMentions.add(curr_men.markSingleton());
        }
      }
      return newMentions;
    }
  }

  public class StrictHeadMatchingSieve implements Sieve {

    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();
      Map<Mention, Set<String>> mentToWordSetMap = new HashMap<Mention, Set<String>>();

      int n = mentions.size();
      if (n == 0) return mentions;

      for (int i = 0; i < n; i++) {
        Entity curr_ent = mentions.get(i).entity;
        Mention curr_men = mentions.get(i).mention;
        curr_men.removeCoreference();

        for (Mention prev_men: mentToWordSetMap.keySet()) {
          Entity prev_ent = prev_men.getCorefferentWith();


          if (curr_men.headToken().isPronoun()) {
            continue;
          }

          if (!curr_men.headToken().lemma().equals(prev_men.headToken().lemma())) {
            continue;
          }

          Set<String> wordSet = mentToWordSetMap.get(prev_men);
          if (wordSet == null) {
            return mentions;
          }

          if (isWordMatch(wordSet, curr_men)) {
            wordSet.addAll(curr_men.toLemmas());
            newMentions.add(curr_men.markCoreferent(prev_ent));
            break;
          }
        }

        if (newMentions.size() < i + 1) {
          newMentions.add(curr_men.markSingleton());
          mentToWordSetMap.put(curr_men, new HashSet<String>(curr_men.toLemmas()));
        }
      }
      // System.out.println(mentions.size() + " " + newMentions.size());
      return newMentions;
    }

    protected boolean isWordMatch(Set<String> wordSet, Mention m) {
      for (Sentence.Token t: m.allTokens()) {
        // System.out.print(t + " " + t.isNoun() + " " + t.lemma() + " " + wordSet);
        if (t.isNoun() && (!wordSet.contains(t.lemma())))
          return false;
      }
      return true;
    }
  }
}
