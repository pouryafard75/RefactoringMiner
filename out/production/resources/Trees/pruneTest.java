class test {
    void main() {
        eventually("There are pending transactions!", new Condition() {
            @Override
            public boolean isSatisfied() throws Exception {
                int localTxCount = transactionTable.getLocalTxCount();
            }
        });
        numbers.forEach( (n) -> {System.out.println(n);});
        numbers.forEach( (n) -> System.out.println(n));
    }
}