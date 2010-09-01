-module(httpclient).
-export([start/3, timer/2, recv/1]).

start(Url, Wait, MaxConn) ->
    inets:start(),
    spawn(?MODULE, timer, [10000, self()]),
    This = self(),
    spawn(fun()-> loadurls(Url, MaxConn, 
            fun(U) -> This ! {loadurl, U} end, Wait) end),
            recv({0,0,0}).

stop() ->
    inets:stop().

recv(Stats) ->
    {Active, Closed, Chunks} = Stats,
    receive
	{stats} -> io:format("Stats: ~w\n",[Stats])
    after 0 -> 
        noop
    end,
    receive
	{http,{_Ref,stream_start,_X}} ->  
	    recv({Active+1,Closed,Chunks});
	{http,{_Ref,stream,_X}} ->          
	    recv({Active, Closed, Chunks+1});
	{http,{_Ref,stream_end,_X}} ->  
	    recv({Active-1, Closed+1, Chunks});
	{http,{_Ref,{error,Why}}} ->
	    io:format("Closed: ~w\n",[Why]),
	    recv({Active-1, Closed+1, Chunks});
	{loadurl, Url} ->
	    http:request(get, {Url, []}, [], [{sync, false}, {stream, self}, {version, 1.1}, {body_format, binary}]),
	    recv(Stats)
    end.

timer(T, Who) ->
    receive
	after T ->
	    Who ! {stats}
	end,
    timer(T, Who).

% Read lines from a file with a specified delay between lines:

for_each(MaxConn, _Callback, _Url) when MaxConn == 0 -> [];
for_each(MaxConn, Callback, Url) -> Callback(Url), for_each(MaxConn -1, Callback, Url).

loadurls(Url, MaxConn, Callback, _Wait) ->
    for_each(MaxConn, Callback, Url).

