package com.icpstep.android.stepcounter;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import org.ic4j.agent.annotations.QUERY;
import org.ic4j.agent.annotations.Waiter;
import org.ic4j.agent.annotations.UPDATE;
import org.ic4j.agent.annotations.Argument;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Type;

public interface HelloWorldProxy {


    @UPDATE
    @Name("greet")
    @Waiter(timeout = 30)
    public CompletableFuture<String> greet(@Argument(Type.TEXT)String name);

    @QUERY
    public CompletableFuture<String> peek();


    @QUERY
    public CompletableFuture<String> hello();

    @UPDATE
    @Name("inc")
    @Waiter(timeout = 30)
    public CompletableFuture<Void> inc();

    @QUERY
    public CompletableFuture<BigInteger> get();

    @QUERY
    @Name("balance")
    @Waiter(timeout = 30)
    public CompletableFuture<BigInteger> balance(@Argument(Type.TEXT)String name);


    @QUERY
    @Name("getImage")
    @Waiter(timeout = 40)
    public CompletableFuture<String> getImage(@Argument(Type.NAT)BigInteger imageId);

    @UPDATE
    @Name("set")
    @Waiter(timeout = 30)
    public CompletableFuture<Void> set(@Argument(Type.NAT)BigInteger n);

    @UPDATE
    @Name("redeem")
    @Waiter(timeout = 30)
    public CompletableFuture<BigInteger> redeem(@Argument(Type.TEXT)String name);

    @UPDATE
    @Name("setImage")
    @Waiter(timeout = 30)
    public CompletableFuture<BigInteger> setImage(@Argument(Type.TEXT)String imageString);


    @UPDATE
    @Name("setPrincipal")
    @Waiter(timeout = 30)
    public CompletableFuture<String> setPrincipal(@Argument(Type.TEXT)String name);
}