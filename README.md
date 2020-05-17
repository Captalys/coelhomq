# coelhomq

You should visit http://localhost:15672/ to see the queues.

Please, go to the namespace `coelhomq.services.consumers` and execute
three functions: `(start)`, `(stop)`, and `(pub-now)`.

What you should look for?

There are four queues:

- **manual-->never-decrease**
- **manual-->never-ask-to-reprocess**
- **manual-->ack-with-delay**
- **auto-->ack-with-delay**

The idea is to understand how different defaults works. The idea of
`manually` controlling the `ack` behavior is great. You can
specifically reprocess messages again in the future for those errors
that happened.


If you want to REPROCESS the messages you need to explicit tell
rabbitmq to RESEND the messages again. For that matter is
important to secure the channel used =) you will need it.


## Usage

```bash
WEBSERVER_PORT=3000 CURRENT_UID=$(id -u):$(id -g) docker-compose up
```

## License

Copyright © 2020 Wanderson Ferreira

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
