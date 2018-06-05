extern crate phorlio;

use phorlio::Response;
use phorlio::PORT_SERVER;
use phorlio::ToBytes;
use phorlio::VERSION;
use std::net::UdpSocket;

fn main() {
    // Clumsy demo code for the school assignment. Must be removed ASAP.
    let socket = UdpSocket::bind(format!("localhost:{}", PORT_SERVER)).unwrap();
    loop {
        let mut buf = Vec::new();
        let (_, src) = socket.recv_from(&mut buf).unwrap();
        let response = Response {
            epoch: 0,
            lifetime: 0,
            result_code: 0,
            version: VERSION
        };
        socket.send_to(&response.to_bytes(), &src).unwrap();
    }
}
