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
    // TODO: before you generate your grammar, the training trees
    // need to be binarized so that rules are at most binary
    List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
    for (int t = 0; t < trainTrees.size(); t++) {
      System.out.println(Trees.PennTreeRenderer.render(trainTrees.get(t)));
      System.out.println(Trees.PennTreeRenderer.render(TreeAnnotations.annotateTree(trainTrees.get(t))));
    }

    // Binarize tree
    for (int t = 0; t < trainTrees.size(); t++) {
      binarizedTrees.add(TreeAnnotations.annotateTree(trainTrees.get(t)));
    }
    lexicon = new Lexicon(binarizedTrees);
    grammar = new Grammar(binarizedTrees);

    System.out.println("Grammar is " + grammar);
  }

  public Tree<String> getBestParse(List<String> sentence) {
    Map<String, Map<String, Double>> probTable = 
      new HashMap<String, Map<String, Double>>();
    Map<String, Map<String, TagInfo>> bestTag = 
      new HashMap<String, Map<String, TagInfo>>();
    buildProbTable(probTable, bestTag, sentence);

    Tree<String> tree = buildTree(bestTag, sentence, "ROOT", 0, sentence.size());
    System.out.println(Trees.PennTreeRenderer.render(tree));
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
      if (tagInfo.getChildTags() == null) {
        tree.setWords(sentence.subList(begin, end));
        return tree;
      } else {
        assert childTags.length == 1;
        // System.out.println("HERE!!!!! " + childTags + childTags[0]);
        childTrees.add(
          buildTree(bestTag, sentence, childTags[0], begin, end)
        );
      }
    } else {
      assert childTags.length == 2;
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
        Map<String, Double> newTagProbs = new HashMap<String, Double>();
        added = false;
        for (String tagB: tagProbs.keySet()) {
          Double tagBProb = tagProbs.get(tagB);
          newTagProbs.put(tagB, tagBProb);
          List<Grammar.UnaryRule> rules = grammar.getUnaryRulesByChild(tagB);
          for (Grammar.UnaryRule rule: rules) {
            String tagA = rule.getParent();
            Double prob = tagProbs.get(tagB) * rule.getScore();
            if (!tagProbs.containsKey(tagA) || prob > tagProbs.get(tagA)) {
              newTagProbs.put(tagA, prob);
              tagInfos.put(tagA, new TagInfo(null, tagB));
              added = true;
            }
          }
        }
        tagProbs = newTagProbs;
      }
      probTable.put(convertToString(i, i + 1), tagProbs);
      bestTag.put(convertToString(i, i + 1), tagInfos);
    }
    System.out.println("prob table initilized " + probTable);
    System.out.println("best tag initilized " + bestTag);
  }

  protected void buildProbTable(
    Map<String, Map<String, Double>> probTable,
    Map<String, Map<String, TagInfo>> bestTag,
    List<String> sentence
  ) {
    initializeProbTable(probTable, bestTag, sentence);
    int n = sentence.size();
    // TODO: check
    for (int span = 2; span <= n; span++) {
      for (int begin = 0; begin <= n - span; begin++) {
        int end = span + begin;
        Map<String, Double> tagProbs = new HashMap<String, Double>();
        Map<String, TagInfo> tagInfos = new HashMap<String, TagInfo>();
        for (int split = begin + 1; split < end; split++) {
          String leftKey = convertToString(begin, split);
          String rightKey = convertToString(split, end);
          // System.out.println("((((((" + this.probTable.containsKey(leftKey));
          // System.out.println("Left key is " + leftKey + this.probTable.get(leftKey));
          // System.out.println("Right key is " + rightKey +  this.probTable.get(rightKey));
          Map<String, Double> leftProbs = probTable.get(leftKey);
          Map<String, Double> rightProbs = probTable.get(rightKey);
          
          for (String leftTag: leftProbs.keySet()) {
            List<Grammar.BinaryRule> rules = 
              this.grammar.getBinaryRulesByLeftChild(leftTag);
            for (Grammar.BinaryRule rule: rules) {
              // System.out.println("rule left: " + rule.getLeftChild());
              // System.out.println("rule right: " + rule.getRightChild());
              // System.out.println("rule parent: " + rule.getParent());

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
                System.out.println("HERE!!!!!************\n");
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
          Map<String, Double> newTagProbs = new HashMap<String, Double>();
          added = false;
          for (String tagB: tagProbs.keySet()) {
            Double tagBProb = tagProbs.get(tagB);
            newTagProbs.put(tagB, tagBProb);

            List<Grammar.UnaryRule> rules = this.grammar.getUnaryRulesByChild(tagB);
            for (Grammar.UnaryRule rule: rules) {
              String tagA = rule.getParent();
              Double prob = tagBProb * rule.getScore();
              if (!tagProbs.containsKey(tagA) || prob > tagProbs.get(tagA)) {
                added = true;
                newTagProbs.put(tagA, prob);
                System.out.println("THERE!!!!!************\n");
                tagInfos.put(tagA, new TagInfo(null, tagB));
              } 
            }
          }
          tagProbs = newTagProbs;
        }
        // System.out.println("key is " + convertToString(begin, end));
        // System.out.println("tagProbs is " + tagProbs);
        probTable.put(convertToString(begin, end), tagProbs);
        bestTag.put(convertToString(begin, end), tagInfos);
      }
    }
    System.out.println("prob table finished " + probTable);
    System.out.println("best tag table is " + bestTag);
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
      return "split: " + this.split + ";" 
        + "child tags: " + tempChildTags + "; \n"; 
    } 
  }
}
