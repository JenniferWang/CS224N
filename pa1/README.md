##PMIAligner
Execute the command
```sh
java -cp ~/cs224n/pa1/java/classes cs224n.assignments.WordAlignmentTester   -dataPath /afs/ir/class/cs224n/data/pa1/   -model cs224n.wordaligner.PMIAligner -evalSet test
```
### Without NULL word:
Language: french
Data path: /afs/ir/class/cs224n/data/pa1//french
Evaluation set: test
Using up to 2147483647 training sentences.
Training set size: 1130104
Evaluation set size: 111
Model: cs224n.wordaligner.PMIAligner
Precision:  0.2280
Recall: 0.1778
AER:  0.7923

### With NULL word:
Precision:  0.1815
Recall: 0.1195
AER: 0.8440

