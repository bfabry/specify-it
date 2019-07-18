Attempted translation of the QuickCheck properties in this paper:

"How to specify it!" - John Hughes

https://www.dropbox.com/s/tx2b84kae4bw1p4/paper.pdf?dl=0

Into Clojure test.check properties. I recreated the first 5 bugs in the paper
(and the test.check properties do fail on them) but got bored after that as they
assumed an implementation of `union` that isn't as silly as mine.
Run the properties by redefining the vars at the top of `bst-spec` to 
point to the implementation of your choice and running `check-props`. There's also a BST that passes all the tests
in `bst`. It's stupidly slow.

Would love some help in how to make the `bst-spec` namespace 

1. have less boilerplate
2. be more idiomatic.
3. I couldn't figure out how to make properties that test shrinking as in the paper
