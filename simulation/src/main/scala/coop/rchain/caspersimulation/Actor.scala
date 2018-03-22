package coop.rchain.caspersimulation

import coop.rchain.caspersimulation.identity.{IdFactory, Identifiable}
import coop.rchain.caspersimulation.network.Network
import coop.rchain.caspersimulation.onchainstate.{Deploy, Resource}
import coop.rchain.caspersimulation.protocol._

sealed abstract class Actor extends Identifiable {
  def takeTurn()(implicit idf: IdFactory): Unit
}

case object Initializer extends Actor {
  override val id: String = "Initializer"

  override def takeTurn()(implicit idf: IdFactory): Unit = Unit //do nothing
}

case class User(id: String, network: Network) extends Actor {

  def requestDeploy()(implicit idf: IdFactory): Unit = {
    val resource = Resource.random
    val deploy = Deploy(resource)
    val msg = DeployRequest(deploy, this)
    network.send(msg)
  }

  override def takeTurn()(implicit idf: IdFactory): Unit = requestDeploy()
}

object User {
  def apply(network: Network)(implicit idf: IdFactory): User = new User(idf.next("user-"), network)
}

case class Validator(id: String, network: Network, state: ProtocolState) extends Actor {

  def newMessage(msg: Message): Unit = {
    state.receive(msg)
  }

  override def takeTurn()(implicit idf: IdFactory): Unit = {
    estimator(this, state).foreach(newBlock => {
      val msg = BlockCreation(newBlock)
      newMessage(msg)
      network.send(msg)
    })
  }

  final def revEarned: Int = {
    //TODO: put in proper reward formula here
    0
  }
}

object Validator {
  def apply(network: Network)(implicit idf: IdFactory): Validator =
    new Validator(idf.next("validator-"), network, ProtocolState.empty)
}