# Distributed Auction

Simulated auction system across a network

## The Program - Distributed Auction

This program simulates a networked auction environment composed of three primary 
components:

- **Bank Server** – Maintains account balances, blocks and transfers funds, and 
registers Auction Houses.
- **Auction House** – Hosts items for bidding, accepts bids from agents, and 
communicates with both the Bank and Agents.
- **Agent** – A user or automated client that browses items, places bids, 
and handles auction events.

### Overview

- Each component runs independently and can be executed on different machines.
- The Bank must be started first and acts as a registry and fund manager.
- Auction Houses register with the Bank on startup.
- Agents query the Bank for available Auction Houses and connect to them for bidding.

## Running the Program

### Start the Bank

The bank must be launched **first** and will stay running throughout the simulation.
* Launch the Bank Server using the command `java - jar bank.jar <port>`

After the Bank is running, the Auction House, Agent and Auto-bidding Agent 
may join and connect to the Bank

### Running an Auction House

An auction house is a client of the bank, but also is a server with agents as 
its clients. Upon creation, the Auction House registers with the bank, opening 
an account with zero balance. It also provides the bank with its host and port 
address so then the Bank can provide Agents with available Auction Houses

* This program takes in three arguments in the command line for the bank's host
name, port, and a random port chosen by the user. To start an Auction
House, you must run `java - jar auctionhouse.jar <bank_host> <bank_port> <port>`
* The Auction House will display its own GUI for the auction house to keep track
of any bidding going on in the house and active sold items. There is no user
interaction on this display, but is only for viewing updates on an Auction House

### Running an Agent

The agent is a client of both the bank and the auction houses. Upon creation, 
it opens a bank account by providing a name and an initial balance, and 
receives a unique account number. The agent gets a list of active auction houses
from the bank. In automatically connects to any open auction house using the 
host and port information sent from the bank. The agent receives a list of 
items being auctioned from the auction house.

* This program takes three arguments in the command line for the bank host name
and port to connect to the Bank. To run the agent, run the command `java - jar 
agent.jar <bank_host> <bank_port>`
* When the jar runs, the user will be prompted in the command line to enter
their name and an initial bank account balance
* The agent will provide a GUI for the user to view all open auction houses and 
bid on any items. The auction houses can be viewed using a scroll pane and the
user may click on any auction house to see the items available for bidding. The
user may also view their total balance and available balance of their bank
account as well as any messages of when you win a bid
* To make a bid, you must click on an item to bid on then enter a bid amount in
the field below and click the bid button to submit a bid.

## Known bugs/things to improve

* There is no full implementation of auto agent bidder
