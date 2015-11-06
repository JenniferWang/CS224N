package cs224n.corefsystems;

import cs224n.ling.Tree;
import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Mention;
import cs224n.coref.Sentence;
import cs224n.coref.Entity;
import cs224n.coref.Util;
import cs224n.util.Pair;
import cs224n.util.CounterMap;
import cs224n.util.Counters;
import java.util.ArrayList;
import java.util.*;

public class RuleBased implements CoreferenceSystem {

  private CounterMap<String, String> headStats;

  @Override
  public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
    headStats = new CounterMap<String, String>();
    for(Pair<Document, List<Entity>> pair : trainingData){
      Document doc = pair.getFirst();
      List<Entity> clusters = pair.getSecond();
      List<Mention> mentions = doc.getMentions();

      for (Entity e: clusters) {
        for (Pair<Mention, Mention> p: e.orderedMentionPairs()) {
          String head1 = p.getFirst().headWord();
          String head2 = p.getSecond().headWord();
          headStats.incrementCount(head1, head2, 1);
          headStats.incrementCount(head2, head1, 1);
        }
      }
    }
    headStats = Counters.conditionalNormalize(headStats);
  }

  @Override
  public List<ClusteredMention> runCoreference(Document doc) {
    List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
    for (Mention m: doc.getMentions()) {
      mentions.add(m.markSingleton());
    }
    ExactMatchSieve exactMatchSieve = new ExactMatchSieve();
    StrictHeadMatchingSieve strictHeadMatchingSeive = new StrictHeadMatchingSieve();
    // SingletonSieve singletonSieve = new SingletonSieve();
    // BaselinePronounSieve baselinePronounSieve = new BaselinePronounSieve();
    HobbsPronounSieve hobbsPronounSeive = new HobbsPronounSieve(doc);
    FuzzyHeadMatchingSieve fuzzyHeadMatchingSeive = new FuzzyHeadMatchingSieve(0.1);
    return passAllSieves(mentions, strictHeadMatchingSeive);
    // return passAllSieves(mentions, exactMatchSieve, strictHeadMatchingSeive, fuzzyHeadMatchingSeive, hobbsPronounSeive);
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

    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();
      int n = mentions.size();
      if (n == 0) return mentions;

      for (int i = 0; i < n; i++) {
        Mention curr_men = mentions.get(i).mention;
        if (!curr_men.headToken().isPronoun()) {
          for (int j = i - 1; j > -1; j--) {
            Mention prev_men = newMentions.get(j).mention;
            if (curr_men.parse.equals(prev_men.parse)) {
              curr_men.removeCoreference();
              newMentions.add(curr_men.markCoreferent(newMentions.get(j)));
              break;
            }
          }
        } 

        if (newMentions.size() < i + 1) {
          newMentions.add(mentions.get(i));
        }
      }
      return newMentions;
    }
  }

  public class FuzzyHeadMatchingSieve implements Sieve {

    protected double threshold;

    public FuzzyHeadMatchingSieve(double threshold) {
      this.threshold = threshold;
    }

    protected boolean propose(ClusteredMention prev_cm, ClusteredMention curr_cm) {
      // System.out.println(treeToMentMap.get(tree).mention.text() + " ** " + treeToMentMap.get(m_root).mention.text());
      Mention prev = prev_cm.mention;
      Mention curr = curr_cm.mention;

      Pair<Boolean, Boolean> gender_res = Util.haveGenderAndAreSameGender(prev, curr);
      if (gender_res.getFirst() && !gender_res.getSecond()) {
        return false;
      }

      Pair<Boolean, Boolean> num_res = Util.haveNumberAndAreSameNumber(prev, curr);
      if (num_res.getFirst() && !num_res.getSecond()) {
        return false;
      }

      if (!prev.headToken().speaker().equals(curr.headToken().speaker())) {
        return false;
      }

      if (!prev.headToken().nerTag().equals(curr.headToken().nerTag())) {
        // System.out.println(tm.text() + " " + tm.headToken().nerTag() + "  " + m.text() + " " + m.headToken().nerTag());
        return false;
      }

      return true;
    }

    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      // System.out.print(headStats);
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();
      for (int i = 0; i < mentions.size(); i++) {
        ClusteredMention curr_cm = mentions.get(i);
        if (!curr_cm.mention.isSingleton()) {
          newMentions.add(curr_cm);
          continue;
        }

        for (int j = i - 1; j > -1; j--) {
          ClusteredMention prev_cm = newMentions.get(j);
          double prob = headStats.getCount(
            curr_cm.mention.headWord(), 
            prev_cm.mention.headWord()
          );
          if (prob > threshold && propose(prev_cm, curr_cm)) {
            Mention m = curr_cm.mention;
            m.removeCoreference();
            newMentions.add(m.markCoreferent(prev_cm));
            break;
          } 
        }
        if (newMentions.size() < i + 1) {
          newMentions.add(curr_cm);
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

          if (curr_men.headToken().isProperNoun() || isWordMatch(wordSet, curr_men)) {
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

  public class BaselinePronounSieve implements Sieve {
    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();

      int n = mentions.size();
      if (n == 0) return mentions;

      for (int i = 0; i < n; i++) {
        Mention curr_men = mentions.get(i).mention;
        if (curr_men.headToken().isPronoun() && curr_men.isSingleton()) {
          for (int j = i - 1; j > -1 ; j--) {
            Mention prev_men = newMentions.get(j).mention;
            if (prev_men.headToken().isNoun()) {
              curr_men.removeCoreference();
              newMentions.add(curr_men.markCoreferent(newMentions.get(j)));
              break;
            }
          }
        }

        if (newMentions.size() < i + 1) {
          newMentions.add(mentions.get(i));
        }
      }
      return newMentions;
    }
  }

  public class HobbsPronounSieve implements Sieve {
    protected String[] nounPronounLabels = {"NP", "NN", "NNS", "NNP", "NNPS", "PRP"};
    protected String[] nounPronounSLabels = {"NP", "NN", "NNS", "NNP", "NNPS", "PRP", "S"};
    protected Map<Tree<String>, ClusteredMention> treeToMentMap;
    protected Document doc;
    public HobbsPronounSieve(Document doc) {
      this.doc = doc;
    }

    public List<ClusteredMention> passSieve(List<ClusteredMention> mentions) {
      List<ClusteredMention> newMentions = new ArrayList<ClusteredMention>();
      int n = mentions.size();
      if (n == 0) return mentions;

      treeToMentMap = new HashMap<Tree<String>, ClusteredMention>();
      for (ClusteredMention m: mentions) {
        treeToMentMap.put(m.mention.parse, m);
      }

      for (int i = 0; i < n; i++) {
        Mention curr_men = mentions.get(i).mention;
        if (curr_men.headToken().isPronoun() && curr_men.isSingleton()) {
          newMentions.add(findClusterByHobbs(mentions.get(i)));
        } else {
          newMentions.add(mentions.get(i));
        }
      }
      return newMentions;
    }

    protected boolean propose(Tree<String> tree, Tree<String> m_root) {
      if (tree.equals(m_root)) return false;
      // System.out.println(treeToMentMap.get(tree).mention.text() + " ** " + treeToMentMap.get(m_root).mention.text());
      try {
        Mention tm = treeToMentMap.get(tree).mention;
        Mention m = treeToMentMap.get(m_root).mention;

        if (tm.headToken().isProperNoun()) return true;

        Pair<Boolean, Boolean> gender_res = Util.haveGenderAndAreSameGender(tm, m);
        if (gender_res.getFirst() && !gender_res.getSecond()) {
          return false;
        }

        Pair<Boolean, Boolean> num_res = Util.haveNumberAndAreSameNumber(tm, m);
        if (num_res.getFirst() && !num_res.getSecond()) {
          return false;
        }

        // same speaker
        if (!tm.headToken().speaker().equals(m.headToken().speaker())) {
          return false;
        }

        // if (!tm.headToken().nerTag().equals(m.headToken().nerTag())) {
        //   System.out.println(tm.text() + " " + tm.headToken().nerTag() + "  " + m.text() + " " + m.headToken().nerTag());
        //   return false;
        // }

      } catch (NullPointerException e) {
        return false;
      }
      return true;
    }

    public ClusteredMention findClusterByHobbs(ClusteredMention pronoun) {
      if (!pronoun.mention.isSingleton()) return pronoun;

      Mention m = pronoun.mention;
      Tree<String> m_root = m.parse;
      boolean isStepTwo = true;

      // Get the path to current mention
      Sentence s = m.sentence;
      Tree<String> s_root = s.parse;
      Map<Tree<String>, Integer> size_map = new HashMap<Tree<String>, Integer>();
      computeTreeSizeMap(s_root, size_map);

      List<Tree<String>> path = new ArrayList<Tree<String>>();
      try{
        buildPathToNode(s_root, m_root, m.beginIndexInclusive, size_map, path);
      } catch (IllegalArgumentException e) {
        // System.out.println("There is a bug!");
        return pronoun;
      }
      
      // Step 1: Begin at the NP immediately dominating the pronoun.
      int begin_idx = path.size() - 1;
      while (begin_idx > -1) {
        if (path.get(begin_idx).isLeaf() || path.get(begin_idx).equalLabelsByOr("NP")) 
          break;
        begin_idx -= 1;
      }

      if (begin_idx < 1) { 
        // System.out.println("Cannot find an NP node!");
        // System.out.println(path);
        return pronoun;
      }

      int x_idx = begin_idx - 1;
      while (x_idx > -1) {
        // Step 2 - 1: Go up tree to first NP or S encountered.
        while (x_idx > -1) {
          if (path.get(x_idx).equalLabelsByOr("NP", "S")) 
            break;
          x_idx -= 1;
        }

        if (x_idx < 0) break;
        // Step 2 - 2: Search left-to-right below X and to left of p, proposing 
        // any NP node which has an NP or S between it and X.
        Tree<String> x = path.get(x_idx);
        Queue<Tree<String>> node_q = new LinkedList<Tree<String>>();
        Queue<Integer> level_q = new LinkedList<Integer>();
        Queue<Integer> npOrS_q = new LinkedList<Integer>();

        node_q.add(x);
        level_q.add(x_idx);
        npOrS_q.add(0);

        while (!node_q.isEmpty()) {
          Tree<String> node = node_q.poll();
          Integer level = level_q.poll();
          Integer count = npOrS_q.poll();

          if (!isStepTwo) {
            // Step 5: If X is an NP, and p does not pass through an N-bar that X 
            // immediately dominates, propose X.
            if (treeToMentMap.containsKey(node) && propose(node, m_root)) {
              return updateMentionAndMap(node, m);
            }
            count += 1;
          } else {
            if (count > 0 && treeToMentMap.containsKey(node) && propose(node, m_root)) {
              return updateMentionAndMap(node, m);
            }
          }

          // bfs left-to-right below X and to left of p
          int increase = node.equalLabelsByOr(nounPronounSLabels) ? 1 : 0;
          for (Tree<String> c: node.getChildren()) {
            node_q.add(c);
            npOrS_q.add(count + increase);
            level_q.add(level + 1);
            if (level + 1 < path.size() && c.equals(path.get(level + 1))) break;
          }
        }

        // If X is an S, search below X to right of p, left-to-right, 
        // breadth-first, but not going through any NP or S, proposing NP 
        // encountered.
        if (x_idx == 0) {
          // TODO: finish this...well, might not be that helpful
          // int p_idx = idxOfChildInTree(x, path.get(1));
          // if (p_idx > 0 && p_idx + 1 < x.getChildren().size()) {
          //   Tree<String> candidate = bfsTree(x.getChildren().get(p_idx + 1), m_root);
          //   if (candidate != null) {
          //     // System.out.println(m + " " + candidate);
          //     return updateMentionAndMap(candidate, m);
          //   }
          // } 
        }
        x_idx -= 1;
        isStepTwo = false;
      }

      // if not find the coreference in current sentence, we search for previous ones
      int idx_sentence = doc.indexOfSentence(m.sentence);
      for (int i = idx_sentence - 1; i > -1; i--) {
        Tree<String> prev_root = doc.sentences.get(i).parse;
        Tree<String> candidate = bfsTree(prev_root, m_root);
        if (candidate == null) continue;
        return updateMentionAndMap(candidate, m);
      }
      return pronoun;
    }

    protected ClusteredMention updateMentionAndMap(Tree<String> t_cluster, Mention m) {
      if (!m.isSingleton()) {
        throw new IllegalArgumentException("Mention is not singleton");
      }
      m.removeCoreference();
      ClusteredMention new_cm = m.markCoreferent(treeToMentMap.get(t_cluster));
      treeToMentMap.put(m.parse, new_cm);
      return new_cm;
    }

    protected int idxOfChildInTree(Tree<String> root, Tree<String> child) {
      for (int i = 0; i < root.getChildren().size(); i++) {
        if (root.getChildren().get(i).equals(child)) {
          return i;
        }
      }
      return -1;
    }

    protected Tree<String> bfsTree(Tree<String> root, Tree<String> m_root) {
      Queue<Tree<String>> queue = new LinkedList<Tree<String>>();
      queue.add(root);
      while (!queue.isEmpty()) {
        Tree<String> node = queue.poll();
        if (treeToMentMap.containsKey(node) && propose(node, m_root)) {
          return node;
        }

        for (Tree<String> child: node.getChildren()) {
          queue.add(child);
        }
      }
      return null; 
    }

    protected void buildPathToNode(
      Tree<String> root, 
      Tree<String> node,
      int num_left_leaves,
      Map<Tree<String>, Integer> size_map,
      List<Tree<String>> path
    ) {
      if (!size_map.containsKey(root) || !size_map.containsKey(node)) {
        throw new IllegalArgumentException("Node cannot find in map");
      }
      if (root.equals(node)) {
        return;
      } 
      int num_leaves = 0;
      for (Tree<String> child: root.getChildren()) {
        if (num_leaves + size_map.get(child) > num_left_leaves) {
          path.add(child);
          buildPathToNode(child, node, num_left_leaves - num_leaves, size_map, path);
          return;
        } else {
          num_leaves += size_map.get(child);
        }
      }
      throw new IllegalArgumentException("Node is not in the tree!");
    }

    protected void computeTreeSizeMap(Tree<String> root, Map<Tree<String>, Integer> map) {
      if (root.isLeaf()) {
        map.put(root, 1);
      } else {
        int numLeaves = 0;
        for (Tree<String> child: root.getChildren()) {
          computeTreeSizeMap(child, map);
          numLeaves += map.get(child);
        }
        map.put(root, numLeaves);
      }
    }
  }
}
