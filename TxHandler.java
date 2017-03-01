import java.util.*;

public class TxHandler {



    public final UTXOPool utxoPool;
    public UTXOPool shadowUtxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
        this.shadowUtxoPool = new UTXOPool();

    }


    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        int i = 0;

        for (Transaction.Output output : tx.getOutputs()) {
            shadowUtxoPool.addUTXO(new UTXO(tx.getHash(), i), output);
            i++;
        }

        // IMPLEMENT THIS
        return     areAllOutputsClaimedInCurrentPool(tx.getInputs())
                && noUTXOClaimedMultipleTimes(tx.getInputs())
                && allOutputValuesPositive(tx.getOutputs())
                && signaturesAreValid(tx)
                && sumOfInputsGtOrEqSumOfOutputs(tx.getInputs(), tx.getOutputs())

                ;
    }

    private boolean signaturesAreValid(Transaction tx ) {

        int i = 0;

        for (Transaction.Input input : tx.getInputs()) {
            Transaction.Output txOutput = utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
            if (txOutput == null ){
                if ( tx.getOutputs().size() > input.outputIndex )
                  if (! Crypto.verifySignature(tx.getOutput(input.outputIndex).address, tx.getRawDataToSign(i), input.signature)) return false;
            }
            else if (! Crypto.verifySignature(txOutput.address, tx.getRawDataToSign(i), input.signature)) return false;
            i++;
        }

        return true;
    }

    private boolean sumOfInputsGtOrEqSumOfOutputs(ArrayList<Transaction.Input> inputs, ArrayList<Transaction.Output> outputs) {

        double outputTotals=0.0;
        double inputTotals=0.0;

        for (Transaction.Output output : outputs) {
            outputTotals  = outputTotals + output.value;

        }

        for (Transaction.Input input : inputs) {
            Transaction.Output txOutput = utxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
            if (txOutput!= null)
                 inputTotals  =inputTotals + txOutput.value;
            else {
                Transaction.Output shadowTxOutput = shadowUtxoPool.getTxOutput(new UTXO(input.prevTxHash, input.outputIndex));
                  inputTotals  = inputTotals + shadowTxOutput.value;

            }
        }

        return inputTotals >= outputTotals;

    }

    public boolean allOutputValuesPositive(ArrayList<Transaction.Output> outputs) {
        boolean retValue = true;

        for (Transaction.Output output : outputs) {
            if (output == null || output.value < 0) {
                retValue =  false;
                break;
            }
        }
        return retValue;
    }



    private boolean noUTXOClaimedMultipleTimes(ArrayList<Transaction.Input> inputs) {

        Map claimedUTXO = new HashMap<UTXO, Integer>();
        for (Transaction.Input input : inputs) {
            UTXO key = new UTXO(input.prevTxHash, input.outputIndex);
            if (claimedUTXO.containsKey(key)) return false;
            claimedUTXO.put(new UTXO(input.prevTxHash, input.outputIndex), 1);
        }
        return true;
    }

    private boolean areAllOutputsClaimedInCurrentPool(ArrayList<Transaction.Input> inputs) {
        for (Transaction.Input input : inputs
                ) {
            if (! utxoPool.contains(new UTXO(input.prevTxHash, input.outputIndex))) return false;
        }
        return true;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        shadowUtxoPool = new UTXOPool();

        List<Transaction> acceptedTxs = new ArrayList<Transaction>();
        for ( Transaction possibleTx:possibleTxs    ) {
            if (isValidTx(possibleTx)){
                for (Transaction.Input input : possibleTx.getInputs()) {
                    utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }

                int i = 0;

                for (Transaction.Output output : possibleTx.getOutputs()) {
                    utxoPool.addUTXO(new UTXO(possibleTx.getHash(), i), output);
                    i++;
                }

                acceptedTxs.add(possibleTx);
            }
        }
        return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

}
