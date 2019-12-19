Future work
=====

Scalability of our batch reader
---

Our reader works as described before in two steps:

* it separates initially from the original file the rows for the number of the expected batches on the main thread

* it executes in parallel the compaction step over the separated rows over the all cores

1. Speeding up the single threaded code by using optimizations
There are optimizations that can be done to speed up the single threaded part: 

1.a. Instead of splitting into rows, the pass that reads the end of line, it can just mark the positions of the end of line(s) and when the multi-threaded code is executed, 
that code would separate in a multi-threaded fashion into rows (or it may even skip allocating these lines all together). 

As we worked with quite limited hardware, the hard disk was especially the limiting factor, so we could just speculate how much 
speed up would generate, but using NVME drives, probably this speed up on reading would show up.

1.b. Another technique which could be explored, is that as long as the compaction happens, we don't read from disk, 
but using a consumer-producer could in fact read in the background using one thread as compaction is happening. 

So in an hypothetical machine where the reading from file takes as much as the compaction, this technique should speed up 
(though, at the price of some extra memory and code complexity) by a factor of 2.

Both techniques 1.a and 1.b could be combined reaching a hypothetical speedup on a balanced machine of more than 2x than our implementation.

2. Instead of exact batches, the SAM file could be read with some expected size, and batching would happen on the chunk of data.

So instead of reading 30K rows of 350 bytes per batch x number of cores x 4 ~= 10 MB x 4 x number of cores, instead we can read:
4 x number of cores buffers of 10 MB (so for a 4 thread machine, it would be like 160 MB) and we will make a batch out of every 10MB buffer in parallel, and will make these batches to be 
around the expected size, but not precise.
This code will be absolutely limited on the speed of reading hardware as there is no computation as reading is done, and after that the processing of splitting 
as in point 1.a can be done in parallel. This approach can be enhanced further that as the parallel batch processing is done, (like described in point 1.b.) 
the background task could read in advance preparing the next section of processing of batches.

Both techniques should improve performance in very fast IO systems which have many cores to be fed to calculate the compaction.

Code complexity should be taken into account for both of those approaches, but it should speed up the code on various machines.

Testing the code using Value Types
----
In 2019 there was a presentation of Value Types (which are semantically similar with struct from C#) that would benefit
between other things the speed of the HashMap. As we use ourselves an equivalent of HashMap to deduplicate Strings, 
some benefits could be extracted out of the original algorithm of compaction.

A bit simpler way to estimate the speed is to code the full implementation using .Net and generic dictionaries, but as
.Net would have (arguably) a weaker GC and code generator, the final results may need adjustment, still some estimation 
could be done on the value or if any speedup could be expected in future.

Doing the algorithms
---
We implemented only reading/writing and some code could be quite easy to be implemented (like filtering) and these algorithms 
could have their own tunings. 

It can be that some algorithms should have special handling, or destroying and re-building the full batch, which would
duplicate the memory per-batch.