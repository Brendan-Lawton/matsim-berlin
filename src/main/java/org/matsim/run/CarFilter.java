package org.matsim.run;


import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.util.*;

class CarFilter {

	public static void main(String[] args) {

		var network = NetworkUtils.readNetwork("/home/brendan/Downloads/berlin-v6.3-network-with-pt.xml.gz");

		// qsim network - not allowing bike infrastrucutre //
		Set<String> modesBike = new HashSet<>();
		modesBike.add("bike");
		Set<String> modesCopy = new HashSet<>();
		modesCopy.add("car");
		modesCopy.add("truck");
		modesCopy.add("ride");
		Set<String> modesInsideRing = new HashSet<>();
		modesInsideRing.add("freight");
		Set<String> modes = new HashSet<>();
		modes.add("freight");
		modes.add("car");
		modes.add("truck");
		modes.add("ride");




		Set<String> restrictedLinkIds = new HashSet<>();
		final String filterShape = "/home/brendan/Downloads/berlin_ring.shp";

		final Collection<Geometry> geometries = new ArrayList<>();


		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(filterShape)) {
			geometries.add((Geometry) feature.getDefaultGeometry());
		}

		for (Link link : new HashSet<>(network.getLinks().values())) { // Create a new HashSet to avoid ConcurrentModificationException

			Coord fromCoord = link.getFromNode().getCoord();
            Coord toCoord = link.getToNode().getCoord();

			if (link.getAllowedModes().contains("bike")) {
				Set<String> linkModes = new HashSet<>(link.getAllowedModes());
				linkModes.remove("bike");
				link.setAllowedModes(linkModes);
			}

			for (Geometry geometry : geometries) {

				if (geometry.contains(MGC.coord2Point(toCoord)) && geometry.contains(MGC.coord2Point(fromCoord))) {
					// Create a copy of the link
					Node fromNode = link.getFromNode();
					Node toNode = link.getToNode();

					Object Bundesstrasse = link.getAttributes().getAttribute("name");
					String testBs = Objects.toString(Bundesstrasse);

					// bundestraßen allowed to remain open to cars. PT links inside the ring should be ignored
					if (!testBs.contains("Bundesstrasse") && !fromNode.getId().toString().contains("pt") && !toNode.getId().toString().contains("pt")) {
						link.setAllowedModes(modesInsideRing);
						Id<Link> newLinkId = Id.createLinkId(link.getId().toString() + "_copy");
						Link linkCopy = network.getFactory().createLink(newLinkId, fromNode, toNode);

						// Set the free speed and capacity to minimum
						linkCopy.setFreespeed(3); // Minimum free speed / very slow to discourage car driving unless needed
						linkCopy.setCapacity(link.getCapacity()); // capacity can remain the same
						linkCopy.setAllowedModes(modesCopy);
						linkCopy.setLength(link.getLength());
						linkCopy.setNumberOfLanes(link.getNumberOfLanes());
						// Add the copied link to the network
						network.addLink(linkCopy);

						restrictedLinkIds.add(link.getId().toString());
					}

				}
			}

			Object autobahn = link.getAttributes().getAttribute("name");
			String testAutobahn = Objects.toString(autobahn);

			if (!link.getFromNode().getId().toString().contains("pt") && !link.getToNode().getId().toString().contains("pt") && !testAutobahn.contains("Autobahn")) {
				Id<Link> newLinkIdBike = Id.createLinkId(link.getId().toString() + "_bike");
				Link linkBike = network.getFactory().createLink(newLinkIdBike, link.getFromNode(), link.getToNode());
				Id<Link> newLinkIdBikeB = Id.createLinkId(link.getId().toString() + "_bike#2");
				Link linkBikeB = network.getFactory().createLink(newLinkIdBikeB, link.getToNode(), link.getFromNode());
				// Set the free speed and capacity to minimum
				linkBike.setFreespeed(4); // Minimum free speed Bikes = 10.6 km/h https://www.berliner-zeitung.de/news/vergleich-berliner-fahrradfahrer-radeln-am-langsamsten-li.177829
				linkBike.setCapacity(1500); // https://pdf.sciencedirectassets.com/308315/1-s2.0-S2352146516X00063/1-s2.0-S2352146516305403/main.pdf?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEIn%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJIMEYCIQCvGoEE8%2Bpr7CsKr7wdPAye%2Bixr4NPywO0AKCs0dU%2BfAgIhAPM%2FWiPhGcEVKOZ5IgJ6ABtgzcwiu%2FARUNUjN8MGJvxyKrsFCKL%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQBRoMMDU5MDAzNTQ2ODY1Igyt5%2F8oj5%2BE8tL4IhEqjwVR3Kyr4dg7EmXd1Zfxml1bmC89AX5fjU9Nuq2gWLQ0cGdAtS%2B0wAJ8sQp9bbTJuTPX75drW7f%2FtoYl%2B61MOm0O2qfLRv6UdXhlWH9FWWTO6bunSWu4PnS3h11OWol67DxYp1bCoeNqQZPRK1KFZdWFTpR2BtzmxjBijkpdJjwNrpTFSV3Ax%2FZ9uVJFHmkFbYyXJWWCDH1U0y2YIYDcIXcUzJ1sTJmLw7u9hmUSIXF5TFMdYKtsPUhSyVVDzi%2BbLIU1%2FD00JDI%2F1LDOi%2FeV8f88h8RBXrJgeO9zFs7%2F1zdhkFKUt9fky4SqsxAcW6Gc3%2BWLIQip2UMlCJSWYZxu7b3glvZe7KmbJzGm9%2BMfUq3Uiu%2Fa0X9DCi60445fO08R9ZH5lwSTog1X1ZXBnWFEjyp9WajVWP2pOpl1eXe8St35F9kpZgEfLHT5Tu5UYW5xjxmATQFEdfr87vxPvCcrcmoxtERShRam0KQvpami7Km4z12IoGz3GSopzJxMJVnTn%2BLO8mkepsoyBYHz1IBZ1%2BC2GWjuTX2aEisp4HHo48sBuziWv8r0evX9%2FESsIkrynlORhx10I7ElvfZM3VMCunNk%2BTae9mFnHQ%2FVOelLhTgMjIf4V%2BaMUefL49BWKdYagKcA2zKu0QN1H%2BFmnsOQkIzZ2gQYSslevOncadw4R%2BlRy3eChcEQsCa0jspfnKmU3DHHU9MATv8HDg5MIHtEwbYJrcwsykCzZVLj0d64Jim6%2Bnc0pdi0MuHcq%2FED%2FuKNecy4eIBmDnTGYK0rdCkFWEeTB8Jo68id7bvzL8F%2FDOB%2FTGaZfohnBP9072YykYIhdpYpUrPfmR00fI7l0TZpbUIUBBOp%2FdmBYarcDxLYEscFMPjr1bYGOrABPUkCXpDWqncXg5KTD0LpiWLhqA6aCwUT3m%2FTW06wn2q1fRBRlDSayhrfqCBapVtjWz3tk9W112y3aCnauLMF2YDdoor5wMSnNcVRKYwnXGbIgu8Jgl25KSzW9pzXwntWOWODoQYkNkqiwShP6HRqMEvs34aZI8ipDNNDhIMLpaw3bTaiRfp66RA4wgYRgUbGck6MvOvteASVb6gQHQUdlMFK1Mmj04D2YM%2F2NPra9WU%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20240902T094604Z&X-Amz-SignedHeaders=host&X-Amz-Expires=300&X-Amz-Credential=ASIAQ3PHCVTYRG7CCKC2%2F20240902%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=28c14250acb5c0ca18d834940fe9914d95ce32d25d3b6305f9e1132c0b5bc3c8&hash=7e30697bd05bd103d7fd48b97329637944b1befa55b4c951831b26b2f6bef9ea&host=68042c943591013ac2b2430a89b270f6af2c76d8dfd086a07176afe7c76c2c61&pii=S2352146516305403&tid=spdf-8b5ffc35-1f99-40c7-b526-4df6693aed61&sid=e4004dd223e75544342ba7199603cb6f546dgxrqb&type=client&tsoh=d3d3LnNjaWVuY2VkaXJlY3QuY29t&ua=1e00590d0d0056525202&rr=8bcc921fb82b7272&cc=de
				linkBikeB.setFreespeed(4); // Minimum free speed Bikes = 10.08 km/h
				linkBikeB.setCapacity(1500); // https://pdf.sciencedirectassets.com/308315/1-s2.0-S2352146516X00063/1-s2.0-S2352146516305403/main.pdf?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEIn%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJIMEYCIQCvGoEE8%2Bpr7CsKr7wdPAye%2Bixr4NPywO0AKCs0dU%2BfAgIhAPM%2FWiPhGcEVKOZ5IgJ6ABtgzcwiu%2FARUNUjN8MGJvxyKrsFCKL%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQBRoMMDU5MDAzNTQ2ODY1Igyt5%2F8oj5%2BE8tL4IhEqjwVR3Kyr4dg7EmXd1Zfxml1bmC89AX5fjU9Nuq2gWLQ0cGdAtS%2B0wAJ8sQp9bbTJuTPX75drW7f%2FtoYl%2B61MOm0O2qfLRv6UdXhlWH9FWWTO6bunSWu4PnS3h11OWol67DxYp1bCoeNqQZPRK1KFZdWFTpR2BtzmxjBijkpdJjwNrpTFSV3Ax%2FZ9uVJFHmkFbYyXJWWCDH1U0y2YIYDcIXcUzJ1sTJmLw7u9hmUSIXF5TFMdYKtsPUhSyVVDzi%2BbLIU1%2FD00JDI%2F1LDOi%2FeV8f88h8RBXrJgeO9zFs7%2F1zdhkFKUt9fky4SqsxAcW6Gc3%2BWLIQip2UMlCJSWYZxu7b3glvZe7KmbJzGm9%2BMfUq3Uiu%2Fa0X9DCi60445fO08R9ZH5lwSTog1X1ZXBnWFEjyp9WajVWP2pOpl1eXe8St35F9kpZgEfLHT5Tu5UYW5xjxmATQFEdfr87vxPvCcrcmoxtERShRam0KQvpami7Km4z12IoGz3GSopzJxMJVnTn%2BLO8mkepsoyBYHz1IBZ1%2BC2GWjuTX2aEisp4HHo48sBuziWv8r0evX9%2FESsIkrynlORhx10I7ElvfZM3VMCunNk%2BTae9mFnHQ%2FVOelLhTgMjIf4V%2BaMUefL49BWKdYagKcA2zKu0QN1H%2BFmnsOQkIzZ2gQYSslevOncadw4R%2BlRy3eChcEQsCa0jspfnKmU3DHHU9MATv8HDg5MIHtEwbYJrcwsykCzZVLj0d64Jim6%2Bnc0pdi0MuHcq%2FED%2FuKNecy4eIBmDnTGYK0rdCkFWEeTB8Jo68id7bvzL8F%2FDOB%2FTGaZfohnBP9072YykYIhdpYpUrPfmR00fI7l0TZpbUIUBBOp%2FdmBYarcDxLYEscFMPjr1bYGOrABPUkCXpDWqncXg5KTD0LpiWLhqA6aCwUT3m%2FTW06wn2q1fRBRlDSayhrfqCBapVtjWz3tk9W112y3aCnauLMF2YDdoor5wMSnNcVRKYwnXGbIgu8Jgl25KSzW9pzXwntWOWODoQYkNkqiwShP6HRqMEvs34aZI8ipDNNDhIMLpaw3bTaiRfp66RA4wgYRgUbGck6MvOvteASVb6gQHQUdlMFK1Mmj04D2YM%2F2NPra9WU%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20240902T094604Z&X-Amz-SignedHeaders=host&X-Amz-Expires=300&X-Amz-Credential=ASIAQ3PHCVTYRG7CCKC2%2F20240902%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=28c14250acb5c0ca18d834940fe9914d95ce32d25d3b6305f9e1132c0b5bc3c8&hash=7e30697bd05bd103d7fd48b97329637944b1befa55b4c951831b26b2f6bef9ea&host=68042c943591013ac2b2430a89b270f6af2c76d8dfd086a07176afe7c76c2c61&pii=S2352146516305403&tid=spdf-8b5ffc35-1f99-40c7-b526-4df6693aed61&sid=e4004dd223e75544342ba7199603cb6f546dgxrqb&type=client&tsoh=d3d3LnNjaWVuY2VkaXJlY3QuY29t&ua=1e00590d0d0056525202&rr=8bcc921fb82b7272&cc=de
				linkBike.setNumberOfLanes(1);
				linkBikeB.setNumberOfLanes(1);
				linkBike.setAllowedModes(modesBike);
				linkBikeB.setAllowedModes(modesBike);
				linkBike.setLength(link.getLength());
				linkBikeB.setLength(link.getLength());
				network.addLink(linkBikeB);
				network.addLink(linkBike);
			}


		}


		NetworkUtils.writeNetwork(network, "input/v6.3/NEW_berlin-v6.3-network-with-pt.xml.gz");

	}
}
