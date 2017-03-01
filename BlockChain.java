// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    TransactionPool currentTransactionPool = new TransactionPool();

    class BlockWithUtxoPool {
        public final Block block;
        public final UTXOPool pool;
        public final long height;


        public BlockWithUtxoPool(Block block, UTXOPool pool, long height) {
            this.block = block;
            this.pool = pool;
            this.height = height;
        }
    }

    Map<ByteArrayWrapper, BlockWithUtxoPool> blocks = new HashMap<ByteArrayWrapper, BlockWithUtxoPool>();


    BlockWithUtxoPool maxHeightBlock = null;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool pool = new UTXOPool();
        Transaction coinbase = genesisBlock.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            pool.addUTXO(utxo, out);
        }

        BlockWithUtxoPool genesis = new BlockWithUtxoPool(genesisBlock, pool, 0);
        maxHeightBlock = genesis;
        blocks.put(new ByteArrayWrapper(genesisBlock.getHash()), genesis);
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return maxHeightBlock.block;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightBlock.pool;
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return currentTransactionPool;

    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * <p>
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS

        BlockWithUtxoPool parentBlock = findBlock(block.getPrevBlockHash());
        if (parentBlock != null &&
                parentBlock.height + 1 > maxHeightBlock.height - CUT_OFF_AGE &&
                allTransactionsValid(block.getTransactions(), parentBlock.pool)
                ) {

            TxHandler txHandler = new TxHandler(new UTXOPool(parentBlock.pool));
            Transaction[] txs = block.getTransactions().toArray(new Transaction[block.getTransactions().size()]);

            txHandler.handleTxs(txs);
            Transaction coinbase = block.getCoinbase();
            txHandler.getUTXOPool().addUTXO(new UTXO(coinbase.getHash(), 0), coinbase.getOutput(0));

            BlockWithUtxoPool newBlock = new BlockWithUtxoPool(block, txHandler.getUTXOPool(), parentBlock.height + 1);
            blocks.put(new ByteArrayWrapper(block.getHash()), newBlock);
            removeTransactionsFromPool(txs, currentTransactionPool);
            if (parentBlock.height + 1 > maxHeightBlock.height) {
                maxHeightBlock = newBlock;
            }

            return true;
        }

        return false;

    }

    private void removeTransactionsFromPool(Transaction[] txs, TransactionPool pool) {

        for (Transaction tx : txs) {
            pool.removeTransaction(tx.getHash());
        }

    }


    private boolean allTransactionsValid(ArrayList<Transaction> transactions, UTXOPool pool) {
        TxHandler handler = new TxHandler(pool);
        boolean valid = true;
        Iterator<Transaction> it = transactions.iterator();
        while (it.hasNext()) {
            if (!handler.isValidTx(it.next())) {
                valid = false;
                break;
            }

        }
        return valid;

    }


    private BlockWithUtxoPool findBlock(byte[] hash) {
        if (hash == null) return null;
        return blocks.get(new ByteArrayWrapper(hash));
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        currentTransactionPool.addTransaction(tx);
    }
}