package cs224n.assignment;

import cs224n.ling.Tree;
import cs224n.ling.Trees;
import java.util.*;

/**
 * The CKY PCFG Parser you will implement.
 */
public class PCFGParser implements Parser {
  private Grammar grammar;
  private Lexicon lexicon;

  public void train(List<Tree<String>> trainTrees) {
    // before you generate your grammar, the training trees
    // need to be binarized so that rules are at most binary
    List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();

    // Binarize tree
    for (int t = 0; t < trainTrees.size(); t++) {
      binarizedTrees.add(TreeAnnotations.annotateTree(trainTrees.get(t)));
    }
    lexicon = new Lexicon(binarizedTrees);
    grammar = new Grammar(binarizedTrees);
  }

  public Tree<String> getBestParse(List<String> sentence) {
    Map<String, Map<String, Double>> probTable = 
      new HashMap<String, Map<String, Double>>();
    Map<String, Map<String, TagInfo>> bestTag = 
      new HashMap<String, Map<String, TagInfo>>();
    buildProbTable(probTable, bestTag, sentence);

    Tree<String> tree = buildTree(bestTag, sentence, "ROOT", 0, sentence.size());
    return TreeAnnotations.unAnnotateTree(tree);
  }

  protected Tree<String> buildTree(
    Map<String, Map<String, TagInfo>>bestTag, 
    List<String> sentence, 
    String currTag,
    int begin,
    int end
  ) {
    String key = convertToString(begin, end);
    TagInfo tagInfo = bestTag.get(key).get(currTag);
    Tree<String> tree = new Tree<String>(currTag);
    List<Tree<String>> childTrees = new ArrayList<Tree<String>>();

    if (tagInfo == null) return null;

    String[] childTags = tagInfo.getChildTags();
    if (tagInfo.getSplit() == null) {
      if (childTags.length == 0) {
        for(String word: sentence.subList(begin, end)) {
          childTrees.add(new Tree<String>(word));
        }
      } else {
        childTrees.add(
          buildTree(bestTag, sentence, childTags[0], begin, end)
        );
      }
    } else {
      childTrees.add(
        buildTree(bestTag, sentence, childTags[0], begin, tagInfo.getSplit())
      );
      childTrees.add(
        buildTree(bestTag, sentence, childTags[1], tagInfo.getSplit(), end)
      );
    }

    tree.setChildren(childTrees);
    return tree;
  }

  protected void initializeProbTable(
    Map<String, Map<String, Double>> probTable,
    Map<String, Map<String, TagInfo>> bestTag,
    List<String> sentence
  ) {
    for (int i = 0; i < sentence.size(); i++) {
      Map<String, Double> tagProbs = new HashMap<String, Double>();
      Map<String, TagInfo> tagInfos = new HashMap<String, TagInfo>();
      for (String tag: lexicon.tagCounter.keySet()) {
        tagProbs.put(
          tag,
          lexicon.scoreTagging(sentence.get(i), tag)
        );
        tagInfos.put(tag, new TagInfo(null));
      }

      // handle unary rule
      Boolean added = true;
      while (added) {
        Set<String> oldTags = new HashSet<String>(tagProbs.keySet());
        added = false;
        for (String tagB: oldTags) {
          Double tagBProb = tagProbs.get(tagB);
          List<Grammar.UnaryRule> rules = grammar.getUnaryRulesByChild(tagB);
          for (Grammar.UnaryRule rule: rules) {
            String tagA = rule.getParent();
            Double prob = tagProbs.get(tagB) * rule.getScore();
            if (!tagProbs.containsKey(tagA) || prob > tagProbs.get(tagA)) {
              tagProbs.put(tagA, prob);
              tagInfos.put(tagA, new TagInfo(null, tagB));
              added = true;
            }
          }
        }
      }
      probTable.put(convertToString(i, i + 1), tagProbs);
      bestTag.put(convertToString(i, i + 1), tagInfos);
    }
  }

  protected void buildProbTable(
    Map<String, Map<String, Double>> probTable,
    Map<String, Map<String, TagInfo>> bestTag,
    List<String> sentence
  ) {
    initializeProbTable(probTable, bestTag, sentence);
    int n = sentence.size();

    for (int span = 2; span <= n; span++) {
      for (int begin = 0; begin <= n - span; begin++) {
        int end = span + begin;
        Map<String, Double> tagProbs = new HashMap<String, Double>();
        Map<String, TagInfo> tagInfos = new HashMap<String, TagInfo>();
        for (int split = begin + 1; split < end; split++) {
          String leftKey = convertToString(begin, split);
          String rightKey = convertToString(split, end);
          Map<String, Double> leftProbs = probTable.get(leftKey);
          Map<String, Double> rightProbs = probTable.get(rightKey);
          
          for (String leftTag: leftProbs.keySet()) {
            List<Grammar.BinaryRule> rules = 
              this.grammar.getBinaryRulesByLeftChild(leftTag);
            for (Grammar.BinaryRule rule: rules) {
              String parentTag = rule.getParent();
              String rightTag = rule.getRightChild();
              if (!rightProbs.containsKey(rightTag)) {
                  continue;
              }
              Double prob = 
                leftProbs.get(leftTag)
                * rightProbs.get(rightTag) 
                * rule.getScore();
       
              if (!tagProbs.containsKey(parentTag) ||
                  tagProbs.get(parentTag) < prob) {
                tagProbs.put(parentTag, prob);
                tagInfos.put(
                  parentTag,
                  new TagInfo(split, leftTag, rightTag)
                );
              }
            }
          }
        }

        // handle unary rule
        Boolean added = true;
        while (added) {
          Set<String> oldTags = new HashSet<String>(tagProbs.keySet());
          added = false;
          for (String tagB: oldTags) {
            Double tagBProb = tagProbs.get(tagB);

            List<Grammar.UnaryRule> rules = this.grammar.getUnaryRulesByChild(tagB);
            for (Grammar.UnaryRule rule: rules) {
              String tagA = rule.getParent();
              Double prob = tagBProb * rule.getScore();
              if (!tagProbs.containsKey(tagA) || prob > tagProbs.get(tagA)) {
                added = true;
                tagProbs.put(tagA, prob);
                tagInfos.put(tagA, new TagInfo(null, tagB));
              } 
            }
          }
        }
        probTable.put(convertToString(begin, end), tagProbs);
        bestTag.put(convertToString(begin, end), tagInfos);
      }
    }
  }

  protected static String convertToString(int ...key) {
    return Arrays.toString(key);
  }

  protected static class TagInfo {
    private Integer split;
    private String[] childTags;

    public TagInfo(Integer split, String ...childTags) {
      this.split = split;
      this.childTags = childTags;
    }
    
    public Integer getSplit() {
      return this.split;
    }

    public String[] getChildTags(){
      return this.childTags;
    }
    
    public String toString() {
      String tempChildTags = "";
      for (String tag: this.childTags) {
        tempChildTags += (tag + ",");
      }
      return " {split: " + this.split + ";" 
        + "child tags: " + tempChildTags + "} \n"; 
    } 
  }
}
